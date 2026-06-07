package com.emeritus.edge_backend;

import com.emeritus.edge_backend.dto.request.TopicAllocationRequest;
import com.emeritus.edge_backend.service.SessionService;
import com.emeritus.edge_backend.service.TenantAllocationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class ConcurrentSchedulingTests {

    @Autowired
    private SessionService sessionService;

    @Autowired
    private TenantAllocationService tenantAllocationService;

    @Test
    void onlyOneSchedulerSucceedsForLastTopicSlot() throws Exception {
        tenantAllocationService.updateAllocations("abc-edu", List.of(
            new TopicAllocationRequest(1L, 1)
        ));

        LocalDate validDate = LocalDate.now().plusDays(21);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startGate = new CountDownLatch(1);

        Future<Result> first = executor.submit(() -> attemptSchedule(startGate, validDate, "concurrent-1"));
        Future<Result> second = executor.submit(() -> attemptSchedule(startGate, validDate, "concurrent-2"));

        startGate.countDown();

        Result firstResult = first.get();
        Result secondResult = second.get();

        executor.shutdown();

        int successCount = 0;
        int slotFullCount = 0;

        for (Result result : List.of(firstResult, secondResult)) {
            if (result.success()) {
                successCount++;
            } else if (result.message().contains("completely full")) {
                slotFullCount++;
            }
        }

        assertEquals(1, successCount, "Exactly one concurrent schedule should succeed");
        assertEquals(1, slotFullCount, "The other scheduler should receive a slot-full error");
    }

    private Result attemptSchedule(CountDownLatch startGate, LocalDate date, String idempotencyKey)
            throws InterruptedException {
        startGate.await();
        try {
            sessionService.scheduleMasterclass(
                "abc-edu", 1L, 1L, date, "Concurrent Session", "Theme", true, List.of(), idempotencyKey);
            return new Result(true, null);
        } catch (IllegalStateException ex) {
            return new Result(false, ex.getMessage());
        }
    }

    private record Result(boolean success, String message) {}
}
