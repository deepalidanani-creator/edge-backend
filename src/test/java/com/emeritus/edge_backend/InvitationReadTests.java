package com.emeritus.edge_backend;

import com.emeritus.edge_backend.entity.Session;
import com.emeritus.edge_backend.service.EmployeeService;
import com.emeritus.edge_backend.service.SessionService;
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
class InvitationReadTests {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private SessionService sessionService;

    private final LocalDate validDate = LocalDate.now().plusDays(21);

    @Test
    void employeeSeesSessionForTheirCohort() {
        sessionService.scheduleMasterclass(
            "vantage-fi", 1L, 1L, validDate, "Cohort Session", "Theme", false,
            List.of("leadership-2026"), "invite-read-1");

        List<Session> sessions = employeeService.getUpcomingSessions("vantage-fi", "emp-001");

        assertEquals(1, sessions.size());
        assertEquals("Cohort Session", sessions.get(0).getTitle());
    }

    @Test
    void employeeDoesNotSeeSessionForOtherCohort() {
        sessionService.scheduleMasterclass(
            "vantage-fi", 1L, 1L, validDate, "Other Cohort Session", "Theme", false,
            List.of("engineering"), "invite-read-2");

        List<Session> sessions = employeeService.getUpcomingSessions("vantage-fi", "emp-001");

        assertTrue(sessions.isEmpty());
    }

    @Test
    void employeeSeesAudienceAllSessions() {
        sessionService.scheduleMasterclass(
            "vantage-fi", 1L, 1L, validDate, "All Hands", "Theme", true,
            List.of(), "invite-read-3");

        List<Session> sessions = employeeService.getUpcomingSessions("vantage-fi", "emp-002");

        assertEquals(1, sessions.size());
        assertEquals("All Hands", sessions.get(0).getTitle());
    }
}
