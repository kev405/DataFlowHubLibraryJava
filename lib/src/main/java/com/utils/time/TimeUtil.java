package com.utils.time;

import java.time.*;

/**
 * Thin wrapper around java.time to enforce UTC storage
 * and provide common conversions.
 */
public final class TimeUtil {

    private static final ZoneId UTC = ZoneOffset.UTC;

    private TimeUtil() { /* utility ‒ no instances */ }

    /** Returns current instant in UTC. Testable via Clock injection. */
    public static Instant nowUtc() {
        return Instant.now(Clock.systemUTC());
    }

    /** Converts an {@link Instant} to a {@link LocalDate} in the given zone. */
    public static LocalDate toLocalDate(Instant instant, ZoneId zone) {
        return instant.atZone(zone).toLocalDate();
    }

    /** ISO-8601 string (always in UTC, Z-suffix). */
    public static String formatIso(Instant instant) {
        return instant.truncatedTo(java.time.temporal.ChronoUnit.MILLIS) // readable
                      .toString();                                       // ISO 8601
    }

    /** Convenience wrapper for {@link Duration#between}. */
    public static Duration between(Instant start, Instant end) {
        return Duration.between(start, end);
    }
}
