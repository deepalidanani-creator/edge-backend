package com.emeritus.edge_backend;

import com.emeritus.edge_backend.dto.response.ScheduleSessionResult;
import com.emeritus.edge_backend.event.LoggingEventPublisher;
import com.emeritus.edge_backend.event.SessionScheduledEvent;
import com.emeritus.edge_backend.repository.SessionRepository;
import com.emeritus.edge_backend.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class IdempotencyTests {

    @Autowired
    private SessionService sessionService;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private LoggingEventPublisher eventPublisher;

    private final LocalDate validDate = LocalDate.now().plusDays(21);

    @BeforeEach
    void clearEvents() {
        eventPublisher.clear();
    }

    @Test
    void retryWithSameIdempotencyKeyReturnsOriginalSession() {
        ScheduleSessionResult first = schedule("idempotent-key-1");
        ScheduleSessionResult second = schedule("idempotent-key-1");

        assertTrue(first.newlyCreated());
        assertFalse(second.newlyCreated());
        assertEquals(first.session().getId(), second.session().getId());
        assertEquals(first.session().getTitle(), second.session().getTitle());
        assertEquals(1, sessionRepository.countByTenantId("vantage-fi"));
    }

    @Test
    void retryDoesNotPublishDuplicateEvent() {
        schedule("idempotent-key-2");
        eventPublisher.clear();

        schedule("idempotent-key-2");

        assertEquals(0, eventPublisher.getPublishedEvents().size());
    }

    @Test
    void differentIdempotencyKeysCreateSeparateSessions() {
        ScheduleSessionResult first = schedule("idempotent-key-3a");
        ScheduleSessionResult second = schedule("idempotent-key-3b");

        assertTrue(first.newlyCreated());
        assertTrue(second.newlyCreated());
        assertEquals(2, sessionRepository.countByTenantId("vantage-fi"));
    }

    private ScheduleSessionResult schedule(String idempotencyKey) {
        return sessionService.scheduleMasterclassWithResult(
            "vantage-fi", 1L, 1L, validDate, "Idempotent Session", "Theme",
            true, List.of(), idempotencyKey);
    }
}
