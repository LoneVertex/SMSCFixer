package com.smscfixer;

import java.util.LinkedHashMap;
import java.util.Map;

/** Minimal synchronized TTL cache with a strict size bound for hook-path metadata. */
final class BoundedTtlCache<K, V> {
    private final int maxEntries;
    private final long ttlMillis;
    private final LinkedHashMap<K, Entry<V>> entries = new LinkedHashMap<>(16, 0.75f, true);

    BoundedTtlCache(int maxEntries, long ttlMillis) {
        if (maxEntries <= 0 || ttlMillis <= 0) {
            throw new IllegalArgumentException("Cache bounds must be positive");
        }
        this.maxEntries = maxEntries;
        this.ttlMillis = ttlMillis;
    }

    synchronized V get(K key, long nowMillis) {
        Entry<V> entry = entries.get(key);
        if (entry == null) {
            return null;
        }
        if (nowMillis - entry.writtenAtMillis >= ttlMillis) {
            entries.remove(key);
            return null;
        }
        return entry.value;
    }

    synchronized void put(K key, V value, long nowMillis) {
        if (!entries.containsKey(key) && entries.size() >= maxEntries) {
            K leastRecentlyUsed = entries.keySet().iterator().next();
            entries.remove(leastRecentlyUsed);
        }
        entries.put(key, new Entry<>(value, nowMillis));
    }

    synchronized int size() {
        return entries.size();
    }

    synchronized void clear() {
        entries.clear();
    }

    private static final class Entry<V> {
        final V value;
        final long writtenAtMillis;

        Entry(V value, long writtenAtMillis) {
            this.value = value;
            this.writtenAtMillis = writtenAtMillis;
        }
    }
}
