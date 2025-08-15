package com.practice.apiservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import com.practice.apiservice.batch.processor.ImportRecordProcessor;
import com.practice.apiservice.batch.processor.RecordValidationException;
import com.practice.apiservice.model.ImportRecord;

class ImportRecordProcessorTest {

    @Test
    void transforms_and_validates_ok() {
        var p = new ImportRecordProcessor("exception", 2, Clock.fixed(Instant.parse("2025-08-14T00:00:00Z"), ZoneOffset.UTC));
        var in = new ImportRecord("A-001","ANA@ACME.COM", new BigDecimal("12.5"), Instant.parse("2025-07-01T12:00:00Z"));
        var out = p.process(in);
        assertThat(out.getUserEmail()).isEqualTo("ana@acme.com");
        assertThat(out.getAmount()).isEqualByComparingTo("12.50");
    }

    @Test
    void duplicate_externalId_in_chunk_throws_or_filters() {
        var p = new ImportRecordProcessor("exception", 2, Clock.systemUTC());
        var a = new ImportRecord("X","a@a.com", new BigDecimal("1"), Instant.now().minus(1, ChronoUnit.DAYS));
        var b = new ImportRecord("X","b@b.com", new BigDecimal("1"), Instant.now().minus(1, ChronoUnit.DAYS));
        p.process(a);
        assertThatThrownBy(() -> p.process(b))
                .isInstanceOf(RecordValidationException.class);
    }
}
