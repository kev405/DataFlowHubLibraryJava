package com;

import org.junit.jupiter.api.Test;

import com.workqueue.WorkQueue;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Multi-thread tests to ensure no task loss and clean shutdown.
 */
class WorkQueueTest {

    @Test
    void producersAndConsumers_processOneThousandTasks() throws Exception {
        WorkQueue wq = new WorkQueue();
        wq.startWorkers(3);                      // 3 consumidores

        int tasks = 1_000;
        AtomicInteger counter = new AtomicInteger();

        ExecutorService producers = Executors.newFixedThreadPool(5); // 5 productores

        for (int i = 0; i < tasks; i++) {
            producers.submit(() -> {
                try {
                    wq.submit(counter::incrementAndGet);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        producers.shutdown();
        assertTrue(producers.awaitTermination(5, TimeUnit.SECONDS));

        wq.stop();                               // bloquea hasta terminar workers
        assertEquals(tasks, counter.get(), "lost tasks detected");
    }

    @Test
    void stop_finishesWithoutDeadlock() throws Exception {
        WorkQueue wq = new WorkQueue();
        wq.startWorkers(2);
        wq.submit(() -> {});                     // dummy task
        long t0 = System.currentTimeMillis();
        wq.stop();
        long elapsed = System.currentTimeMillis() - t0;
        assertTrue(elapsed < 2000, "stop took too long");
    }
}
