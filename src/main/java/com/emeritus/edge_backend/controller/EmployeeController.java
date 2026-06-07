package com.emeritus.edge_backend.controller;

import com.emeritus.edge_backend.entity.Session;
import com.emeritus.edge_backend.service.EmployeeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping("/v1/employees/{id}/upcoming-sessions")
    public ResponseEntity<?> getUpcomingSessions(
            @RequestHeader(value = "X-Tenant-Id", required = true) String tenantId,
            @PathVariable("id") String employeeId) {

        try {
            List<Session> sessions = employeeService.getUpcomingSessions(tenantId, employeeId);
            return ResponseEntity.ok(sessions);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}
