package com.emeritus.edge_backend.event;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Published when a scheduled session is cancelled.
 *
 * JSON schema:
 * {
 *   "eventType": "session.cancelled",
 *   "occurredAt": "2026-06-06T10:35:00Z",
 *   "tenantId": "vantage-fi",
 *   "sessionId": 42,
 *   "topicId": 1,
 *   "sessionDate": "2026-07-01",
 *   "title": "AI Strategy Workshop"
 * }
 */
public record SessionCancelledEvent(
    Instant occurredAt,
    String tenantId,
    Long sessionId,
    Long topicId,
    LocalDate sessionDate,
    String title
) implements DomainEvent {

    @Override
    public String eventType() {
        return "session.cancelled";
    }
}
