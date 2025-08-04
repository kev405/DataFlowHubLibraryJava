package com;

import org.junit.jupiter.api.Test;

import com.utils.time.TimeUtil;

import java.time.*;

import static org.junit.jupiter.api.Assertions.*;

class TimeUtilTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota"); // UTC-05:00

    @Test
    void toLocalDate_convertsCorrectlyBogota() {
        Instant   utcMidnight = Instant.parse("2025-08-15T05:00:00Z"); // 00:00 Bogotá
        LocalDate local       = TimeUtil.toLocalDate(utcMidnight, BOGOTA);
        assertEquals(LocalDate.of(2025, 8, 15), local);
    }

    @Test
    void formatIso_outputsUtcZSuffix() {
        Instant   ts  = Instant.parse("2025-01-01T12:34:56.789Z");
        String iso    = TimeUtil.formatIso(ts);
        assertEquals("2025-01-01T12:34:56.789Z", iso);
    }

    @Test
    void between_returnsExactDuration() {
        Instant start = Instant.parse("2025-01-01T00:00:00Z");
        Instant end   = start.plusSeconds(3600);
        assertEquals(3600, TimeUtil.between(start, end).getSeconds());
    }
}
