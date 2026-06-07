package com.emeritus.edge_backend;

import com.emeritus.edge_backend.event.AllocationUpdatedEvent;
import com.emeritus.edge_backend.event.LoggingEventPublisher;
import com.emeritus.edge_backend.event.SessionCancelledEvent;
import com.emeritus.edge_backend.event.SessionScheduledEvent;
import com.emeritus.edge_backend.service.SessionService;
import com.emeritus.edge_backend.service.TenantAllocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class EventPublicationTests {

    @Autowired
    private LoggingEventPublisher eventPublisher;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private TenantAllocationService tenantAllocationService;

    private final LocalDate validDate = LocalDate.now().plusDays(21);

    @BeforeEach
    void clearEvents() {
        eventPublisher.clear();
    }

    @Test
    void schedulePublishesSessionScheduledEvent() {
        SessionScheduledEvent event = scheduleOneSession();

        assertEquals("session.scheduled", event.eventType());
        assertEquals("vantage-fi", event.tenantId());
        assertEquals(1L, event.topicId());
    }

    @Test
    void cancelPublishesSessionCancelledEvent() {
        SessionScheduledEvent scheduled = scheduleOneSession();
        eventPublisher.clear();

        sessionService.cancelSession("vantage-fi", scheduled.sessionId());

        assertEquals(1, eventPublisher.getPublishedEvents().size());
        assertTrue(eventPublisher.getPublishedEvents().get(0) instanceof SessionCancelledEvent);
        SessionCancelledEvent cancelled = (SessionCancelledEvent) eventPublisher.getPublishedEvents().get(0);
        assertEquals(scheduled.sessionId(), cancelled.sessionId());
    }

    @Test
    void updateAllocationsPublishesAllocationUpdatedEvent() {
        tenantAllocationService.updateAllocations("vantage-fi", List.of(
            new com.emeritus.edge_backend.dto.request.TopicAllocationRequest(1L, 2),
            new com.emeritus.edge_backend.dto.request.TopicAllocationRequest(2L, 1)
        ));

        assertEquals(1, eventPublisher.getPublishedEvents().size());
        assertTrue(eventPublisher.getPublishedEvents().get(0) instanceof AllocationUpdatedEvent);
        AllocationUpdatedEvent event = (AllocationUpdatedEvent) eventPublisher.getPublishedEvents().get(0);
        assertEquals("allocation.updated", event.eventType());
        assertEquals(2, event.allocations().size());
    }

    private SessionScheduledEvent scheduleOneSession() {
        sessionService.scheduleMasterclass(
            "vantage-fi", 1L, 1L, validDate, "Event Test", "Theme", false,
            List.of("leadership-2026"), "event-pub-1");

        assertEquals(1, eventPublisher.getPublishedEvents().size());
        return (SessionScheduledEvent) eventPublisher.getPublishedEvents().get(0);
    }
}
