package com.emeritus.edge_backend.service;

import com.emeritus.edge_backend.dto.response.ScheduleSessionResult;
import com.emeritus.edge_backend.entity.Session;
import com.emeritus.edge_backend.entity.TenantAllocation;
import com.emeritus.edge_backend.entity.TenantBucket;
import com.emeritus.edge_backend.event.EventPublisher;
import com.emeritus.edge_backend.event.SessionCancelledEvent;
import com.emeritus.edge_backend.event.SessionScheduledEvent;
import com.emeritus.edge_backend.repository.SessionRepository;
import com.emeritus.edge_backend.repository.SpeakerTopicRepository;
import com.emeritus.edge_backend.repository.TenantAllocationRepository;
import com.emeritus.edge_backend.repository.TenantBucketRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class SessionService {

    private final TenantAllocationRepository allocationRepository;
    private final SessionRepository sessionRepository;
    private final TenantBucketRepository bucketRepository;
    private final SpeakerTopicRepository speakerTopicRepository;
    private final EventPublisher eventPublisher;

    public SessionService(TenantAllocationRepository allocationRepository,
                          SessionRepository sessionRepository,
                          TenantBucketRepository bucketRepository,
                          SpeakerTopicRepository speakerTopicRepository,
                          EventPublisher eventPublisher) {
        this.allocationRepository = allocationRepository;
        this.sessionRepository = sessionRepository;
        this.bucketRepository = bucketRepository;
        this.speakerTopicRepository = speakerTopicRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Session scheduleMasterclass(String tenantId, Long topicId, Long speakerId,
                                       LocalDate date, String title, String theme,
                                       boolean audienceAll, List<String> cohortIds,
                                       String idempotencyKey) {
        return scheduleMasterclassWithResult(
            tenantId, topicId, speakerId, date, title, theme, audienceAll, cohortIds, idempotencyKey
        ).session();
    }

    @Transactional
    public ScheduleSessionResult scheduleMasterclassWithResult(
            String tenantId, Long topicId, Long speakerId,
            LocalDate date, String title, String theme,
            boolean audienceAll, List<String> cohortIds,
            String idempotencyKey) {

        return sessionRepository.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey)
            .map(existing -> new ScheduleSessionResult(existing, false))
            .orElseGet(() -> new ScheduleSessionResult(
                createMasterclass(tenantId, topicId, speakerId, date, title, theme,
                    audienceAll, cohortIds, idempotencyKey),
                true));
    }

    private Session createMasterclass(String tenantId, Long topicId, Long speakerId,
                                      LocalDate date, String title, String theme,
                                      boolean audienceAll, List<String> cohortIds,
                                      String idempotencyKey) {

        if (date.isBefore(LocalDate.now().plusDays(14))) {
            throw new IllegalArgumentException("Masterclass must be scheduled at least 14 days in the future.");
        }

        List<String> normalizedCohortIds = validateAudience(audienceAll, cohortIds);

        if (!speakerTopicRepository.existsBySpeakerIdAndTopicId(speakerId, topicId)) {
            throw new IllegalArgumentException(
                "Speaker " + speakerId + " is not eligible to deliver topic " + topicId + ".");
        }

        TenantBucket bucket = bucketRepository.findByTenantIdForUpdate(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Invalid Tenant ID specified."));

        TenantAllocation allocation = allocationRepository.findByTenantIdAndTopicId(tenantId, topicId)
            .orElseThrow(() -> new IllegalArgumentException("No capacity has been allocated for this topic."));

        if (allocation.getAllocatedSlots() <= 0) {
            throw new IllegalArgumentException("No capacity has been allocated for this topic.");
        }

        long totalScheduledSessions = sessionRepository.countByTenantId(tenantId);
        if (totalScheduledSessions >= bucket.getMaxLimit()) {
            throw new IllegalStateException(
                "Global subscription tier limit reached (" + bucket.getMaxLimit() + "). Upgrade required.");
        }

        long existingTopicSessions = sessionRepository.countByTenantIdAndTopicId(tenantId, topicId);
        if (existingTopicSessions >= allocation.getAllocatedSlots()) {
            throw new IllegalStateException(
                "All allocated slots (" + allocation.getAllocatedSlots() + ") for this topic are completely full.");
        }

        Session newSession = new Session();
        newSession.setTenantId(tenantId);
        newSession.setTopicId(topicId);
        newSession.setSpeakerId(speakerId);
        newSession.setSessionDate(date);
        newSession.setTitle(title);
        newSession.setTheme(theme);
        newSession.setAudienceAll(audienceAll);
        newSession.setCohortIds(normalizedCohortIds);
        newSession.setIdempotencyKey(idempotencyKey);

        Session savedSession;
        try {
            savedSession = sessionRepository.save(newSession);
        } catch (DataIntegrityViolationException ex) {
            return sessionRepository.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey)
                .orElseThrow(() -> ex);
        }

        eventPublisher.publish(new SessionScheduledEvent(
            Instant.now(),
            tenantId,
            savedSession.getId(),
            savedSession.getTopicId(),
            savedSession.getSpeakerId(),
            savedSession.getSessionDate(),
            savedSession.getTitle(),
            savedSession.getTheme(),
            savedSession.isAudienceAll(),
            savedSession.getCohortIds()
        ));

        return savedSession;
    }

    private List<String> validateAudience(boolean audienceAll, List<String> cohortIds) {
        boolean hasCohorts = cohortIds != null && !cohortIds.isEmpty();

        if (audienceAll && hasCohorts) {
            throw new IllegalArgumentException(
                "Audience must be all employees or specific cohorts, not both.");
        }
        if (!audienceAll && !hasCohorts) {
            throw new IllegalArgumentException(
                "Audience must be all employees or specific cohorts, not neither.");
        }
        if (audienceAll) {
            return new ArrayList<>();
        }
        return cohortIds;
    }

    @Transactional(readOnly = true)
    public List<Session> getSessionsByTenant(String tenantId) {
        return sessionRepository.findByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public Session getSessionById(String tenantId, Long sessionId) {
        return sessionRepository.findByIdAndTenantId(sessionId, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Session not found."));
    }

    @Transactional
    public void cancelSession(String tenantId, Long sessionId) {
        Session session = sessionRepository.findByIdAndTenantId(sessionId, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Session not found."));

        eventPublisher.publish(new SessionCancelledEvent(
            Instant.now(),
            tenantId,
            session.getId(),
            session.getTopicId(),
            session.getSessionDate(),
            session.getTitle()
        ));

        sessionRepository.delete(session);
    }
}
