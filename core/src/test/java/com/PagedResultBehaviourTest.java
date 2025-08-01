package com;

import org.junit.jupiter.api.Test;

import com.practice.domain.utils.pagination.PagedResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Behaviour tests for {@link PagedResult}.
 */
class PagedResultBehaviourTest {

    // ---------- happy-path --------------------------------------------------

    @Test
    void constructor_storesArgumentsAndMapPreservesTotal() {
        List<Integer> page = List.of(1, 2, 3);
        PagedResult<Integer> pr = new PagedResult<>(page, 0, 3, 10);

        assertEquals(0, pr.page());
        assertEquals(3, pr.size());
        assertEquals(10, pr.total());
        assertEquals(page, pr.items());

        // map() should transform items and keep paging metadata
        PagedResult<String> mapped = pr.map(Object::toString);
        assertEquals(List.of("1", "2", "3"), mapped.items());
        assertEquals(pr.total(), mapped.total());
        assertEquals(pr.page(), mapped.page());
    }

    @Test
    void items_areReturnedAsUnmodifiableCopy() {
        PagedResult<Integer> pr = new PagedResult<>(List.of(1), 0, 10, 1);
        assertThrows(UnsupportedOperationException.class,
                     () -> pr.items().add(99));
    }

    // ---------- validation error paths -------------------------------------

    @Test
    void negativePage_isRejected() {
        assertThrows(IllegalArgumentException.class,
                     () -> new PagedResult<>(List.of(), -1, 10, 0));
    }

    @Test
    void zeroOrNegativeSize_isRejected() {
        assertThrows(IllegalArgumentException.class,
                     () -> new PagedResult<>(List.of(), 0, 0, 0));
    }

    @Test
    void itemsLargerThanSize_isRejected() {
        List<Integer> items = List.of(1, 2);
        assertThrows(IllegalArgumentException.class,
                     () -> new PagedResult<>(items, 0, 1, 2));
    }
}
