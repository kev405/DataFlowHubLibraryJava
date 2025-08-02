package com.utils.generic;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generic collection helpers that rely on PECS (Producer Extends, Consumer Super)
 * to avoid unchecked casts and raw types.
 */
public final class CollectionUtils {

    private CollectionUtils() { }

    /**
     * Returns an immutable copy of the given list.
     *
     * @param src   source list (producer) – may be any subtype of T
     * @param <T>   element type of the resulting list
     * @return      unmodifiable copy preserving iteration order
     */
    public static <T> List<T> copy(List<? extends T> src) {
        return List.copyOf(src);
    }

    /**
     * Adds every element from {@code src} into {@code dst}.
     *
     * @param dst destination (consumer) – may accept any supertype of T
     * @param src source (producer)      – may produce any subtype of T
     * @param <T> common upper-bound type
     */
    public static <T> void addAll(Collection<? super T> dst,
                                  Collection<? extends T> src) {
        dst.addAll(src);
    }

    /**
     * Performs a deep unmodifiable copy of a map: the map itself plus
     * each contained collection value becomes unmodifiable.
     */
    public static <K, V> Map<K, V> deepUnmodifiable(Map<? extends K, ? extends V> src) {
        Map<K, V> tmp = new HashMap<>();
        src.forEach((k, v) -> tmp.put(k, v));
        return Map.copyOf(tmp);
    }
}
