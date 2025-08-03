package com.utils.error;

import org.slf4j.Logger;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Generic error-formatting helper.
 * <p>
 * No domain dependencies — only SLF4J.
 * </p>
 */
public final class ErrorHandler {

    private ErrorHandler() { /* util class */ }

    /* ------------------------------------------------------------ */
    /* Public API                                                   */
    /* ------------------------------------------------------------ */

    /**
     * Builds a concise String for logs:
     * <pre>
     * [IllegalStateException] boom
     * com.example.Foo.bar(Foo.java:42)
     * ...
     * </pre>
     *
     * @param t        throwable to format
     * @param verbose  true = whole stack-trace, false = first 5 lines
     */
    public static String format(Throwable t, boolean verbose) {
        String header = String.format("[%s] %s",
                                      t.getClass().getSimpleName(),
                                      t.getMessage());
        String stack  = stackTraceAsString(t, verbose ? Integer.MAX_VALUE : 5);
        return stack.isBlank() ? header : header + '\n' + stack;
    }

    /**
     * Convenience wrapper around {@code logger.error(..)}.
     */
    public static void log(Logger log, Throwable t, boolean verbose) {
        log.error(format(t, verbose));
    }

    /* ------------------------------------------------------------ */
    /* helpers                                                      */
    /* ------------------------------------------------------------ */

    private static String stackTraceAsString(Throwable t, int maxLines) {
        return Arrays.stream(t.getStackTrace())
                     .limit(maxLines)
                     .map(StackTraceElement::toString)
                     .collect(Collectors.joining("\n"));
    }
}
