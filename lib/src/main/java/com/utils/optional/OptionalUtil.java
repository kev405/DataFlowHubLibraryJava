package com.utils.optional;

import java.util.Arrays;
import java.util.Optional;

/**
 * Optional helpers that reduce boilerplate and promote
 * "no Optional.get() without isPresent()" rule.
 */
public final class OptionalUtil {

    private OptionalUtil() { }   // utility class

    /**
     * Returns the first non-empty {@link Optional} in the provided arguments,
     * or {@link Optional#empty()} if all are empty.
     *
     * @param opts var-arg list of Optionals
     * @param <T>  type inside the Optional
     */
    @SafeVarargs
    public static <T> Optional<T> firstPresent(Optional<T>... opts) {
        return Arrays.stream(opts)
                     .filter(Optional::isPresent)
                     .map(Optional::get)
                     .findFirst();
    }
}
