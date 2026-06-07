package com.emeritus.edge_backend.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * In-process stub for a real message bus (Kafka, SQS, etc.).
 * Logs events and keeps an in-memory copy for tests and local inspection.
 */
@Component
public class LoggingEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingEventPublisher.class);

    private final List<DomainEvent> publishedEvents = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void publish(DomainEvent event) {
        log.info("Published domain event: type={}, tenant={}, payload={}",
            event.eventType(), event.tenantId(), event);
        publishedEvents.add(event);
    }

    public List<DomainEvent> getPublishedEvents() {
        synchronized (publishedEvents) {
            return List.copyOf(publishedEvents);
        }
    }

    public void clear() {
        publishedEvents.clear();
    }
}
