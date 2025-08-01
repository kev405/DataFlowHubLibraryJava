package com.utils.cache;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TtlCache<K, V> {

    private record Entry<V>(V value, Instant expiresAt) {}

    private final ConcurrentMap<K, Entry<V>> map = new ConcurrentHashMap<>();
    private final Duration ttl;
    private final ScheduledExecutorService sweeper;

    public TtlCache(Duration ttl, Duration sweepInterval) {
        this.ttl = ttl;
        this.sweeper = Executors.newSingleThreadScheduledExecutor();
        sweeper.scheduleAtFixedRate(this::purgeExpired,
                                    sweepInterval.toMillis(),
                                    sweepInterval.toMillis(),
                                    TimeUnit.MILLISECONDS);
    }

    public void put(K key, V value) {
        map.put(key, new Entry<>(value, Instant.now().plus(ttl)));
    }

    public Optional<V> get(K key) {
        return Optional.ofNullable(map.get(key))
                       .filter(e -> e.expiresAt().isAfter(Instant.now()))
                       .map(Entry::value);
    }

    public int size() { purgeExpired(); return map.size(); }

    public void shutdown() { sweeper.shutdownNow(); }

    private void purgeExpired() {
        Instant now = Instant.now();
        map.entrySet().removeIf(e -> e.getValue().expiresAt().isBefore(now));
    }
    
}
