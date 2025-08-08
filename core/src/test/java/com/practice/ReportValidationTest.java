package com.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.practice.domain.processing.ProcessingRequest;
import com.practice.domain.report.Report;

public class ReportValidationTest {
    @Test
    void generatedAt_mustBeAfterRequest() {
        ProcessingRequest req = TestFixtures.newPendingRequest();
        Instant before = req.createdAt().minusSeconds(1);
        assertThrows(IllegalArgumentException.class,
                    () -> new Report(UUID.randomUUID(), req, "/r", "{}", before,
                                    TestFixtures.sampleUser()));
    }

    @Test
    void buildsValidReport() {
        var req = TestFixtures.newPendingRequest();
        Report rpt = new Report(UUID.randomUUID(), req,
                                "/reports/out.pdf",
                                "{\"rows\":42}",
                                Instant.now().plusSeconds(1),
                                TestFixtures.sampleUser());
        assertEquals(req, rpt.processingRequest());
    }

    @Test
    void blankPath_isRejected() {
        var req = TestFixtures.newPendingRequest();
        assertThrows(IllegalArgumentException.class,
                     () -> new Report(UUID.randomUUID(), req,
                                      " ", "{}", Instant.now(), 
                                      TestFixtures.sampleUser()));
    }
}
