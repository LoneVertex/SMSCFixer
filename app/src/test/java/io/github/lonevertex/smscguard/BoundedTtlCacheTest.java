package io.github.lonevertex.smscguard;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class BoundedTtlCacheTest {
    @Test
    public void returnsValueBeforeExpiryAndDropsItAtExpiry() {
        BoundedTtlCache<String, String> cache = new BoundedTtlCache<>(2, 100L);
        cache.put("one", "value", 1_000L);
        assertEquals("value", cache.get("one", 1_099L));
        assertNull(cache.get("one", 1_100L));
        assertEquals(0, cache.size());
    }

    @Test
    public void evictsLeastRecentlyUsedEntryAtCapacity() {
        BoundedTtlCache<String, String> cache = new BoundedTtlCache<>(2, 1_000L);
        cache.put("one", "first", 1L);
        cache.put("two", "second", 2L);
        assertEquals("first", cache.get("one", 3L));
        cache.put("three", "third", 4L);
        assertEquals("first", cache.get("one", 5L));
        assertNull(cache.get("two", 5L));
        assertEquals("third", cache.get("three", 5L));
    }

    @Test
    public void clearRemovesAllEntries() {
        BoundedTtlCache<Integer, Integer> cache = new BoundedTtlCache<>(2, 1_000L);
        cache.put(1, 1, 1L);
        cache.clear();
        assertNull(cache.get(1, 2L));
        assertEquals(0, cache.size());
    }
}
