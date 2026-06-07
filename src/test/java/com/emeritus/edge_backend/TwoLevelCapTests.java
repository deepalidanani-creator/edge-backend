package com.emeritus.edge_backend;

import com.emeritus.edge_backend.dto.request.TopicAllocationRequest;
import com.emeritus.edge_backend.service.SessionService;
import com.emeritus.edge_backend.service.TenantAllocationService;
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
class TwoLevelCapTests {

    @Autowired
    private TenantAllocationService tenantAllocationService;

    @Autowired
    private SessionService sessionService;

    @Test
    void putAllocationCannotExceedBucketCap() {
        List<TopicAllocationRequest> requests = List.of(
            new TopicAllocationRequest(1L, 2),
            new TopicAllocationRequest(2L, 2)
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> tenantAllocationService.updateAllocations("vantage-fi", requests));

        assertEquals("Total allocated slots (4) exceeds bucket cap (3).", ex.getMessage());
    }

    @Test
    void scheduleCannotExceedTopicAllocation() {
        LocalDate validDate = LocalDate.now().plusDays(21);

        sessionService.scheduleMasterclass(
            "vantage-fi", 1L, 1L, validDate, "Session 1", "Theme", true, List.of(), "cap-test-1");
        sessionService.scheduleMasterclass(
            "vantage-fi", 1L, 1L, validDate.plusDays(1), "Session 2", "Theme", true, List.of(), "cap-test-2");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> sessionService.scheduleMasterclass(
                "vantage-fi", 1L, 1L, validDate.plusDays(2), "Session 3", "Theme", true, List.of(), "cap-test-3"));

        assertEquals("All allocated slots (2) for this topic are completely full.", ex.getMessage());
    }

    @Test
    void scheduleCannotExceedGlobalBucketCap() {
        tenantAllocationService.updateAllocations("vantage-fi", List.of(
            new TopicAllocationRequest(1L, 1),
            new TopicAllocationRequest(2L, 1),
            new TopicAllocationRequest(3L, 1)
        ));

        LocalDate validDate = LocalDate.now().plusDays(21);

        sessionService.scheduleMasterclass(
            "vantage-fi", 1L, 1L, validDate, "Global 1", "Theme", true, List.of(), "global-cap-1");
        sessionService.scheduleMasterclass(
            "vantage-fi", 2L, 2L, validDate.plusDays(1), "Global 2", "Theme", true, List.of(), "global-cap-2");
        sessionService.scheduleMasterclass(
            "vantage-fi", 3L, 2L, validDate.plusDays(2), "Global 3", "Theme", true, List.of(), "global-cap-3");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> sessionService.scheduleMasterclass(
                "vantage-fi", 1L, 1L, validDate.plusDays(3), "Global 4", "Theme", true, List.of(), "global-cap-4"));

        assertEquals("Global subscription tier limit reached (3). Upgrade required.", ex.getMessage());
    }
}
