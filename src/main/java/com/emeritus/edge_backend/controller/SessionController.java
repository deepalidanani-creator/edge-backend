package com.emeritus.edge_backend.controller;

import com.emeritus.edge_backend.entity.Session;
import com.emeritus.edge_backend.service.SessionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping
    public List<Session> getSessions(@RequestHeader(value = "X-Tenant-Id", required = true) String tenantId) {
        return sessionService.getSessionsByTenant(tenantId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getSession(
            @RequestHeader(value = "X-Tenant-Id", required = true) String tenantId,
            @PathVariable("id") Long id) {
        try {
            return ResponseEntity.ok(sessionService.getSessionById(tenantId, id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> scheduleSession(
            @RequestHeader(value = "X-Tenant-Id", required = true) String tenantId,
            @RequestHeader(value = "X-User-Role", required = true) String userRole,
            @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
            @RequestBody Map<String, Object> payload) {

        if (!"LND_ADMIN".equalsIgnoreCase(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Access Denied: Only LND_ADMIN can schedule sessions."));
        }

        try {
            Long topicId = Long.valueOf(payload.get("topicId").toString());
            Long speakerId = Long.valueOf(payload.get("speakerId").toString());
            LocalDate date = LocalDate.parse(payload.get("date").toString());
            String title = payload.get("title").toString();
            String theme = payload.get("theme").toString();
            boolean audienceAll = Boolean.parseBoolean(payload.get("audienceAll").toString());

            @SuppressWarnings("unchecked")
            List<String> cohortIds = (List<String>) payload.get("cohortIds");

            var result = sessionService.scheduleMasterclassWithResult(
                tenantId, topicId, speakerId, date, title, theme, audienceAll, cohortIds, idempotencyKey
            );

            HttpStatus status = result.newlyCreated() ? HttpStatus.CREATED : HttpStatus.OK;
            return ResponseEntity.status(status).body(result.session());

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelSession(
            @RequestHeader(value = "X-Tenant-Id", required = true) String tenantId,
            @RequestHeader(value = "X-User-Role", required = true) String userRole,
            @PathVariable("id") Long id) {

        if (!"LND_ADMIN".equalsIgnoreCase(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Access Denied: Only LND_ADMIN can cancel sessions."));
        }

        try {
            sessionService.cancelSession(tenantId, id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}
