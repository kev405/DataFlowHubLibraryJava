package com.utils.cache;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe in-memory cache with a Least Recently Used (LRU) eviction policy.
 *
 * @param <K> Key
 * @param <V> Value
 */
public class LruCache<K, V> {
    private final int capacity;
    private final Map<K, V> map;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /* optional metrics */
    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);

    public LruCache(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity <= 0");
        this.capacity = capacity;
        this.map = new LinkedHashMap<>(16, .75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return super.size() > LruCache.this.capacity;
            }
        };
    }

    // ---------- basic ops ---------------------------------------------------

    /** Associates the specified value with the given key (thread-safe). */
    public void put(K key, V value) {
        lock.writeLock().lock();
        try { map.put(key, value); }
        finally { lock.writeLock().unlock(); }
    }

    /** Returns an {@link Optional} with the cached value or empty. */
    public Optional<V> get(K key) {
        lock.readLock().lock();
        try {
            V val = map.get(key);
            if (val == null) {
                missCount.incrementAndGet();
            } else {
                hitCount.incrementAndGet();
            }
            return Optional.ofNullable(val);
        } finally { lock.readLock().unlock(); }
    }

    public Optional<V> remove(K key) {
        lock.writeLock().lock();
        try { return Optional.ofNullable(map.remove(key)); }
        finally { lock.writeLock().unlock(); }
    }

    public int size() {
        lock.readLock().lock();
        try { return map.size(); }
        finally { lock.readLock().unlock(); }
    }

    public void clear() {
        lock.writeLock().lock();
        try { map.clear(); }
        finally { lock.writeLock().unlock(); }
    }

    // ---------- optional metrics -------------------------------------------

    public long hitCount()  { return hitCount.get();  }
    public long missCount() { return missCount.get(); }
}
