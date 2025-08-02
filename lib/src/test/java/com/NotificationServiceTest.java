package com;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import com.entity.Notification;
import com.service.NotificationService;

class NotificationServiceTest {

    private static final NotificationService service = new NotificationService(4);

    @AfterAll
    static void tearDown() throws InterruptedException {
        service.shutdown();
        assertTrue(service.awaitTermination(2, TimeUnit.SECONDS));
    }

    @Test
    void sendHundredNotifications_inParallel_completeSuccessfully() throws Exception {
        List<Future<Boolean>> futures = new CopyOnWriteArrayList<>();
        AtomicInteger counter = new AtomicInteger();

        for (int i = 0; i < 100; i++) {
            Notification n = new Notification("user" + i, "Job #" + i);
            futures.add(service.send(n));
        }

        // wait all
        for (Future<Boolean> f : futures) {
            assertTrue(f.get(5, TimeUnit.SECONDS));
            counter.incrementAndGet();
        }
        assertEquals(100, counter.get());
    }

    @Test
    void shutdown_finishesUnderTwoSeconds() throws InterruptedException {
        long t0 = System.currentTimeMillis();
        try (NotificationService tmp = new NotificationService(2)) {
            tmp.send(new Notification("a", "b"));
        }
        long elapsed = System.currentTimeMillis() - t0;
        assertTrue(elapsed < 2_000);
    }
}
