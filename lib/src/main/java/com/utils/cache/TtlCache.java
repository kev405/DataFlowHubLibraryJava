package com.utils.cache;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Simple TTL cache. Each entry expires after {@code ttl}.
 * A background sweeper periodically removes expired entries.
 *
 * @param <K> Key
 * @param <V> Value
 */
public class TtlCache<K, V> {

    /* ---------- internal entry ---------- */
    private record Entry<V>(V value, Instant expiresAt) {}

    private final ConcurrentMap<K, Entry<V>> map = new ConcurrentHashMap<>();
    private final Duration ttl;
    private final ScheduledExecutorService sweeper;

    /* ---------------------------------------------------------------------- */
    /* Constructors                                                           */
    /* ---------------------------------------------------------------------- */

    public TtlCache(Duration ttl, Duration sweepInterval) {
        this.ttl = ttl;
        this.sweeper = newExecutor();                 // <-- factory method
        sweeper.scheduleAtFixedRate(this::purgeExpired,
                                    sweepInterval.toMillis(),
                                    sweepInterval.toMillis(),
                                    TimeUnit.MILLISECONDS);
    }

    /** Protected factory so tests can override and inject a failing executor. */
    protected ScheduledExecutorService newExecutor() {
        return Executors.newSingleThreadScheduledExecutor();
    }

    /* ---------------------------------------------------------------------- */
    /* Public API                                                             */
    /* ---------------------------------------------------------------------- */

    public void put(K key, V value) {
        map.put(key, new Entry<>(value, Instant.now().plus(ttl)));
    }

    public Optional<V> get(K key) {
        return Optional.ofNullable(map.get(key))
                       .filter(e -> e.expiresAt().isAfter(Instant.now()))
                       .map(Entry::value);
    }

    public int size() {
        purgeExpired();
        return map.size();
    }

    public void shutdown() { sweeper.shutdownNow(); }

    /* ---------------------------------------------------------------------- */
    /* Helpers                                                                */
    /* ---------------------------------------------------------------------- */

    void purgeExpired() {                // package-private for tests
        Instant now = Instant.now();
        map.entrySet().removeIf(e -> e.getValue().expiresAt().isBefore(now));
    }
    
}
