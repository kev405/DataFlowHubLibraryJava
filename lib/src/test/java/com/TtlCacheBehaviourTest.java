package com;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.utils.cache.TtlCache;

/**
 * Behaviour and concurrency tests for {@link TtlCache}.
 */
public class TtlCacheBehaviourTest {
    
    /** Small TTL = 50 ms, sweeper runs every 20 ms. */
    private final TtlCache<String, String> cache =
            new TtlCache<>(Duration.ofMillis(50),
                           Duration.ofMillis(20));

    @AfterEach
    void shutdown() { cache.shutdown(); }

    // -------------------------------------------------------------------- //
    // 1) Expiration behaviour                                              //
    // -------------------------------------------------------------------- //

    @Test
    void entryExpires_afterTtl() throws InterruptedException {
        cache.put("k", "v");

        // Immediately present
        assertEquals(Optional.of("v"), cache.get("k"));

        // Wait a little longer than TTL and trigger purge via size()
        Thread.sleep(70);
        cache.size();                   // purgeExpired() inside
        assertTrue(cache.get("k").isEmpty(), "Entry should be expired");
    }

    // -------------------------------------------------------------------- //
    // 2) Basic thread-safety under parallel load                            //
    // -------------------------------------------------------------------- //

    @Test
    void cache_isThreadSafe_duringPutAndGet() throws Exception {
        final int threads = 8;
        ExecutorService exec = Executors.newFixedThreadPool(threads);

        Callable<Void> task = () -> {
            for (int i = 0; i < 500; i++) {
                String k = UUID.randomUUID().toString();
                cache.put(k, "val");
                assertTrue(cache.get(k).isPresent());
            }
            return null;
        };

        for (int i = 0; i < threads; i++) exec.submit(task);
        exec.shutdown();
        assertTrue(exec.awaitTermination(5, TimeUnit.SECONDS),
                   "Executor did not finish in time");
    }
    
}
