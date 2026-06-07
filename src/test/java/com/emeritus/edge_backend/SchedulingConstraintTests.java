package com.emeritus.edge_backend;

import com.emeritus.edge_backend.service.SessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class SchedulingConstraintTests {

    @Autowired
    private SessionService sessionService;

    private final LocalDate validDate = LocalDate.now().plusDays(21);

    @Test
    void scheduleRejectsSpeakerNotEligibleForTopic() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> sessionService.scheduleMasterclass(
                "vantage-fi", 1L, 2L, validDate, "Title", "Theme", true, List.of(), "speaker-topic-1"));

        assertEquals("Speaker 2 is not eligible to deliver topic 1.", ex.getMessage());
    }

    @Test
    void scheduleRejectsAudienceAllWithCohorts() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> sessionService.scheduleMasterclass(
                "vantage-fi", 1L, 1L, validDate, "Title", "Theme", true,
                List.of("leadership-2026"), "audience-xor-1"));

        assertEquals("Audience must be all employees or specific cohorts, not both.", ex.getMessage());
    }

    @Test
    void scheduleRejectsNeitherAudienceAllNorCohorts() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> sessionService.scheduleMasterclass(
                "vantage-fi", 1L, 1L, validDate, "Title", "Theme", false,
                List.of(), "audience-xor-2"));

        assertEquals("Audience must be all employees or specific cohorts, not neither.", ex.getMessage());
    }

    @Test
    void scheduleRejectsDateWithin14Days() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> sessionService.scheduleMasterclass(
                "vantage-fi", 1L, 1L, LocalDate.now().plusDays(7),
                "Title", "Theme", true, List.of(), "date-rule-1"));

        assertEquals("Masterclass must be scheduled at least 14 days in the future.", ex.getMessage());
    }
}
