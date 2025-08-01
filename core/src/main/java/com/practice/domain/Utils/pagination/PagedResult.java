package com.practice.domain.utils.pagination;

import java.util.List;
import java.util.function.Function;

public record PagedResult<T>(
    List<T> items,
    int     page,
    int     size,
    long    total
) {
    public PagedResult {
        if (items == null) throw new NullPointerException("items");
        if (page < 0 || size <= 0 || total < 0)
            throw new IllegalArgumentException("invalid paging arguments");
        if (items.size() > size)
            throw new IllegalArgumentException("items exceed page size");
        items = List.copyOf(items);
    }

    /** Maps each item and returns a new immutable PagedResult. */
    @SuppressWarnings("unchecked")
    public <R> PagedResult<R> map(Function<? super T, ? extends R> f) {
        var mapped = (List<R>) items.stream().map(f).toList();
        return new PagedResult<>(mapped, page, size, total);
    }
}
