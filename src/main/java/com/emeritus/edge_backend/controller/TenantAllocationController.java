package com.emeritus.edge_backend.controller;

import com.emeritus.edge_backend.dto.request.TopicAllocationRequest;
import com.emeritus.edge_backend.entity.TenantAllocation;
import com.emeritus.edge_backend.service.TenantAllocationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/tenants/{id}/allocations")
public class TenantAllocationController {

    private final TenantAllocationService tenantAllocationService;

    public TenantAllocationController(TenantAllocationService tenantAllocationService) {
        this.tenantAllocationService = tenantAllocationService;
    }

    @GetMapping
    public List<TenantAllocation> getAllocations(@PathVariable("id") String tenantId) {
        return tenantAllocationService.getAllocations(tenantId);
    }

    @PutMapping
    public ResponseEntity<?> updateAllocations(
            @PathVariable("id") String tenantId,
            @RequestHeader(value = "X-User-Role", required = true) String userRole,
            @RequestBody List<TopicAllocationRequest> requests) {

        if (!"LND_ADMIN".equalsIgnoreCase(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Access Denied: Only LND_ADMIN can update allocations."));
        }

        try {
            List<TenantAllocation> allocations = tenantAllocationService.updateAllocations(tenantId, requests);
            return ResponseEntity.ok(allocations);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
}
