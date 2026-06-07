package com.emeritus.edge_backend.event;

import java.time.Instant;

/**
 * Base contract for domain events published to downstream services
 * (notifications, CSM dashboard, calendar integration).
 */
public interface DomainEvent {

    String eventType();

    Instant occurredAt();

    String tenantId();
}
