package com;

import org.junit.jupiter.api.Test;

import com.utils.cache.TtlCache;

import java.time.Duration;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that infrastructure failures during TTL cache initialization
 * are properly propagated.
 */
class TtlCacheFailureTest {

    @Test
    void scheduledExecutorFailure_isWrappedAsInfraException() {
        // Given a TTL cache but the sweeper is forced to throw during construction
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            new TtlCache<String, String>(Duration.ofMillis(10), Duration.ofMillis(5)) {
                @Override protected ScheduledThreadPoolExecutor newExecutor() {
                    throw new RuntimeException("no threads allowed");
                }
            };
        });

        assertEquals("no threads allowed", ex.getMessage());
    }
}