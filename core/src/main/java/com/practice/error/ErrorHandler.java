package com.practice.error;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.slf4j.Logger;

import com.practice.exception.DatabaseUnavailableException;
import com.practice.exception.InfraException;
import com.practice.exception.InvalidFileFormatException;
import com.practice.exception.UserNotFoundException;

/**
 * Centralised helper for formatting and logging exceptions.
 */
public final class ErrorHandler {

    private ErrorHandler() { }

    /* --------------------------------------------------
       Public API
       -------------------------------------------------- */

    /**
     * Builds a concise string containing:
     *  * an error code (see {@link ErrorCode})
     *  * simple class name
     *  * exception message
     *  * root-cause message (if different)
     *
     * @param t       exception to format
     * @param verbose whether to include the full stack-trace
     *                (false ⇒ first 5 lines only)
     */
    public static String format(Throwable t, boolean verbose) {
        Throwable root = getRootCause(t);
        String stack = stackTraceAsString(t, verbose ? Integer.MAX_VALUE : 5);
        
        // Include root cause message if different from main exception
        String rootCauseInfo = "";
        if (root != t && !root.getMessage().equals(t.getMessage())) {
            rootCauseInfo = String.format(" (caused by: %s)", root.getMessage());
        }
        
        return String.format("[%s] %s: %s%s\n%s",
                errorCode(t),
                t.getClass().getSimpleName(),
                t.getMessage(),
                rootCauseInfo,
                stack.isEmpty() ? "" : stack);
    }

    /**
     * Convenience wrapper that logs the formatted error with
     * {@code logger.error(..)}.
     */
    public static void log(Logger logger, Throwable t, boolean verbose) {
        logger.error(format(t, verbose));
    }

    /* --------------------------------------------------
       Internal helpers
       -------------------------------------------------- */

    /** Returns the deepest cause (never null). */
    static Throwable getRootCause(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null) cur = cur.getCause();
        return cur;
    }

    /** Maps exception to an error code for dashboards / logs. */
    static ErrorCode errorCode(Throwable t) {
        if (t instanceof InvalidFileFormatException) return ErrorCode.INVALID_FILE;
        if (t instanceof DatabaseUnavailableException) return ErrorCode.DB_DOWN;
        if (t instanceof UserNotFoundException)        return ErrorCode.USER_NOT_FOUND;
        if (t instanceof InfraException)               return ErrorCode.INFRA_GENERIC;
        return ErrorCode.UNKNOWN;
    }

    /** Converts stack trace to string, limited to {@code maxLines}. */
    private static String stackTraceAsString(Throwable t, int maxLines) {
        return Arrays.stream(t.getStackTrace())
                     .limit(maxLines)
                     .map(StackTraceElement::toString)
                     .collect(Collectors.joining("\n"));
    }

    /* --------------------------------------------------
       Error code enum
       -------------------------------------------------- */

    public enum ErrorCode {
        INVALID_FILE,
        DB_DOWN,
        USER_NOT_FOUND,
        INFRA_GENERIC,
        UNKNOWN
    }
}
