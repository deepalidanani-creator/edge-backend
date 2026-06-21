package com.emeritus.edge_backend.service;

import com.emeritus.edge_backend.entity.Employee;
import com.emeritus.edge_backend.entity.Session;
import com.emeritus.edge_backend.repository.EmployeeRepository;
import com.emeritus.edge_backend.repository.SessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Invitation read pattern:
 * 1. Load employee cohort memberships (typically a small list).
 * 2. Query tenant-scoped upcoming sessions in two indexed paths:
 *    - audience_all sessions for the tenant
 *    - cohort-targeted sessions overlapping the employee's cohorts
 * 3. Merge and sort by session date.
 *
 * Product decision — cohort membership changes after scheduling:
 * We use CURRENT membership. If an employee joins a cohort after a session was
 * scheduled for that cohort, they will see the invitation on their next LMS read.
 * This matches employee expectations in a live LMS. The alternative (point-in-time
 * snapshot at schedule time) is better for audit/compliance but worse for UX and
 * requires a materialized invitation table; we would add that at scale.
 *
 * At scale (1000 cohorts / 50 sessions): add composite index on
 * (tenant_id, session_date), index session_cohorts(cohort_id), and consider a
 * denormalized session_invitation table maintained by event consumers.
 */
@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final SessionRepository sessionRepository;

    public EmployeeService(EmployeeRepository employeeRepository, SessionRepository sessionRepository) {
        this.employeeRepository = employeeRepository;
        this.sessionRepository = sessionRepository;
    }

    @Transactional(readOnly = true)
    public List<Session> getUpcomingSessions(String tenantId, String employeeId) {
        Employee employee = employeeRepository.findByIdAndTenantId(employeeId, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Employee not found."));

        LocalDate today = LocalDate.now();
        List<String> cohortIds = employee.getCohortIds();

        List<Session> audienceAllSessions =
            sessionRepository.findUpcomingAudienceAllSessions(tenantId, today);

        if (cohortIds == null || cohortIds.isEmpty()) {
            return audienceAllSessions;
        }

        List<Session> cohortSessions =
            sessionRepository.findUpcomingCohortTargetedSessions(tenantId, today, cohortIds);

        return mergeAndSort(audienceAllSessions, cohortSessions);
    }

    private List<Session> mergeAndSort(List<Session> audienceAllSessions, List<Session> cohortSessions) {
        Map<Long, Session> merged = new LinkedHashMap<>();
        for (Session session : audienceAllSessions) {
            merged.put(session.getId(), session);
        }
        for (Session session : cohortSessions) {
            merged.putIfAbsent(session.getId(), session);
        }
        List<Session> result = new ArrayList<>(merged.values());
        result.sort(Comparator.comparing(Session::getSessionDate));
        return result;
    }
    
	
}
