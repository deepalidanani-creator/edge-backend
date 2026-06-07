package com.emeritus.edge_backend.event;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Published when a masterclass session is successfully scheduled.
 *
 * JSON schema:
 * {
 *   "eventType": "session.scheduled",
 *   "occurredAt": "2026-06-06T10:30:00Z",
 *   "tenantId": "vantage-fi",
 *   "sessionId": 42,
 *   "topicId": 1,
 *   "speakerId": 1,
 *   "sessionDate": "2026-07-01",
 *   "title": "AI Strategy Workshop",
 *   "theme": "Leadership",
 *   "audienceAll": false,
 *   "cohortIds": ["leadership-2026"]
 * }
 */
public record SessionScheduledEvent(
    Instant occurredAt,
    String tenantId,
    Long sessionId,
    Long topicId,
    Long speakerId,
    LocalDate sessionDate,
    String title,
    String theme,
    boolean audienceAll,
    List<String> cohortIds
) implements DomainEvent {

    @Override
    public String eventType() {
        return "session.scheduled";
    }
}
