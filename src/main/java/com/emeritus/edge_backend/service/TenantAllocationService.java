package com.emeritus.edge_backend.service;

import com.emeritus.edge_backend.dto.request.TopicAllocationRequest;
import com.emeritus.edge_backend.entity.TenantAllocation;
import com.emeritus.edge_backend.entity.TenantBucket;
import com.emeritus.edge_backend.event.AllocationUpdatedEvent;
import com.emeritus.edge_backend.event.EventPublisher;
import com.emeritus.edge_backend.repository.SessionRepository;
import com.emeritus.edge_backend.repository.TenantAllocationRepository;
import com.emeritus.edge_backend.repository.TenantBucketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TenantAllocationService {

    private final TenantAllocationRepository allocationRepository;
    private final TenantBucketRepository bucketRepository;
    private final SessionRepository sessionRepository;
    private final EventPublisher eventPublisher;

    public TenantAllocationService(TenantAllocationRepository allocationRepository,
                                   TenantBucketRepository bucketRepository,
                                   SessionRepository sessionRepository,
                                   EventPublisher eventPublisher) {
        this.allocationRepository = allocationRepository;
        this.bucketRepository = bucketRepository;
        this.sessionRepository = sessionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<TenantAllocation> getAllocations(String tenantId) {
        validateTenantExists(tenantId);
        return sortedAllocations(allocationRepository.findByTenantId(tenantId));
    }

    @Transactional
    public List<TenantAllocation> updateAllocations(String tenantId, List<TopicAllocationRequest> requests) {
        TenantBucket bucket = bucketRepository.findByTenantIdForUpdate(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Invalid Tenant ID specified."));

        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("At least one topic allocation is required.");
        }

        for (TopicAllocationRequest request : requests) {
            if (request.topicId() == null) {
                throw new IllegalArgumentException("topicId is required for each allocation.");
            }
            if (request.allocatedSlots() < 0) {
                throw new IllegalArgumentException("Allocated slots cannot be negative.");
            }
        }

        int totalAllocated = requests.stream()
            .filter(request -> request.allocatedSlots() > 0)
            .mapToInt(TopicAllocationRequest::allocatedSlots)
            .sum();

        if (totalAllocated > bucket.getMaxLimit()) {
            throw new IllegalArgumentException(
                "Total allocated slots (" + totalAllocated + ") exceeds bucket cap (" + bucket.getMaxLimit() + ").");
        }

        Set<Long> topicsToKeep = new HashSet<>();
        for (TopicAllocationRequest request : requests) {
            if (request.allocatedSlots() > 0) {
                topicsToKeep.add(request.topicId());
            }
        }

        for (TopicAllocationRequest request : requests) {
            if (request.allocatedSlots() == 0) {
                validateNoScheduledSessions(tenantId, request.topicId());
            } else {
                validateAllocationNotBelowScheduled(tenantId, request.topicId(), request.allocatedSlots());
            }
        }

        for (TenantAllocation existing : allocationRepository.findByTenantId(tenantId)) {
            if (!topicsToKeep.contains(existing.getTopicId())) {
                validateNoScheduledSessions(tenantId, existing.getTopicId());
            }
        }

        allocationRepository.findByTenantId(tenantId).stream()
            .filter(existing -> !topicsToKeep.contains(existing.getTopicId()))
            .forEach(allocationRepository::delete);

        for (TopicAllocationRequest request : requests) {
            if (request.allocatedSlots() == 0) {
                allocationRepository.findByTenantIdAndTopicId(tenantId, request.topicId())
                    .ifPresent(allocationRepository::delete);
                continue;
            }

            TenantAllocation allocation = allocationRepository.findByTenantIdAndTopicId(tenantId, request.topicId())
                .orElse(new TenantAllocation(tenantId, request.topicId(), 0));
            allocation.setAllocatedSlots(request.allocatedSlots());
            allocationRepository.save(allocation);
        }

        List<TenantAllocation> updated = sortedAllocations(allocationRepository.findByTenantId(tenantId));

        eventPublisher.publish(new AllocationUpdatedEvent(
            Instant.now(),
            tenantId,
            updated.stream()
                .map(a -> new AllocationUpdatedEvent.TopicAllocationSnapshot(
                    a.getTopicId(), a.getAllocatedSlots()))
                .toList()
        ));

        return updated;
    }

    private void validateAllocationNotBelowScheduled(String tenantId, Long topicId, int newSlots) {
        long scheduledSessions = sessionRepository.countByTenantIdAndTopicId(tenantId, topicId);
        if (newSlots < scheduledSessions) {
            throw new IllegalStateException(
                "Cannot reduce topic " + topicId + " below " + scheduledSessions + " scheduled session(s).");
        }
    }

    private void validateNoScheduledSessions(String tenantId, Long topicId) {
        long scheduledSessions = sessionRepository.countByTenantIdAndTopicId(tenantId, topicId);
        if (scheduledSessions > 0) {
            throw new IllegalStateException(
                "Cannot remove allocation for topic " + topicId + " with " + scheduledSessions + " scheduled session(s).");
        }
    }

    private List<TenantAllocation> sortedAllocations(List<TenantAllocation> allocations) {
        return allocations.stream()
            .sorted(Comparator.comparing(TenantAllocation::getTopicId))
            .toList();
    }

    private void validateTenantExists(String tenantId) {
        bucketRepository.findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Invalid Tenant ID specified."));
    }
}
