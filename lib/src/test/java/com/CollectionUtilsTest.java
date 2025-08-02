package com;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.utils.generic.CollectionUtils;

/**
 * Contract tests for {@link CollectionUtils}.
 */
class CollectionUtilsTest {

    @Test
    void copy_returnsImmutableList() {
        List<Integer> src = List.of(1, 2, 3);
        List<Number> copy = CollectionUtils.copy(src);

        assertEquals(src, copy);
        assertThrows(UnsupportedOperationException.class,
                     () -> copy.add(4));
    }

    @Test
    void addAll_acceptsSubtypesViaWildcards() {
        List<Integer> ints = List.of(1, 2, 3);
        Collection<Number> nums = new ArrayList<>();
        CollectionUtils.addAll(nums, ints);  // ? super / ? extends

        assertTrue(nums.containsAll(ints));
    }

    @Test
    void deepUnmodifiable_returnsDefensiveCopy() {
        Map<String, Integer> src = Map.of("a", 1);
        Map<String, Integer> unmod = CollectionUtils.deepUnmodifiable(src);

        assertEquals(src, unmod);
        assertThrows(UnsupportedOperationException.class,
                     () -> unmod.put("b", 2));
    }
}
