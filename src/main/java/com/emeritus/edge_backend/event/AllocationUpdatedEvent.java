package com.emeritus.edge_backend.event;

import java.time.Instant;
import java.util.List;

/**
 * Published when a tenant's topic allocations are updated.
 *
 * JSON schema:
 * {
 *   "eventType": "allocation.updated",
 *   "occurredAt": "2026-06-06T10:20:00Z",
 *   "tenantId": "vantage-fi",
 *   "allocations": [
 *     { "topicId": 1, "allocatedSlots": 2 },
 *     { "topicId": 2, "allocatedSlots": 1 }
 *   ]
 * }
 */
public record AllocationUpdatedEvent(
    Instant occurredAt,
    String tenantId,
    List<TopicAllocationSnapshot> allocations
) implements DomainEvent {

    @Override
    public String eventType() {
        return "allocation.updated";
    }

    public record TopicAllocationSnapshot(Long topicId, int allocatedSlots) {}
}
