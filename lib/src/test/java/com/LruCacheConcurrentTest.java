package com;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.utils.cache.LruCache;

/** Concurrency and eviction tests for {@link LruCache}. */
public class LruCacheConcurrentTest {
    
     @Test
    void lruEvictsOldestWhenCapacityExceeded() {
        LruCache<Integer, String> cache = new LruCache<>(2);
        cache.put(1, "A");
        cache.put(2, "B");
        cache.put(3, "C"); // debe expulsar la clave 1
        assertTrue(cache.get(1).isEmpty(), "oldest entry was not evicted");
        assertEquals(2, cache.size());
    }

    @Test
    void cache_isThreadSafeUnderLoad() throws Exception {
        final int capacity = 100;
        LruCache<UUID, Integer> cache = new LruCache<>(capacity);
        ExecutorService exec = Executors.newFixedThreadPool(10);

        Callable<Void> task = () -> {
            for (int i = 0; i < 1_000; i++) {
                UUID key = UUID.randomUUID();
                cache.put(key, i);
                cache.get(key);
                cache.remove(key);
            }
            return null;
        };

        for (int i = 0; i < 10; i++) exec.submit(task);
        exec.shutdown();
        assertTrue(exec.awaitTermination(10, TimeUnit.SECONDS));

        assertTrue(cache.size() <= capacity,
                   "cache size exceeds capacity after stress test");
    }
    
}
