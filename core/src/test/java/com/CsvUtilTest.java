package com;

import org.junit.jupiter.api.*;

import com.practice.domain.processing.ProcessingRequest;
import com.practice.io.csv.CsvUtil;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvUtilTest {

    private static Path tmp;

    @BeforeAll static void prep() throws Exception {
        tmp = Files.createTempFile("req", ".csv");
    }
    @AfterAll  static void clean() throws Exception { Files.deleteIfExists(tmp); }

    @Test
    void writeAndRead_utf8_semicolonDelimiter() throws Exception {
        List<ProcessingRequest> in = List.of(
                TestFixtures.newPendingRequest(),
                TestFixtures.newPendingRequest());

        CsvUtil.writeRequests(tmp, in, StandardCharsets.UTF_8, ';');
        List<ProcessingRequest> out =
                CsvUtil.readRequests(tmp, StandardCharsets.UTF_8, ';');

        assertEquals(in.size(), out.size());
        assertEquals(in.get(0).id(), out.get(0).id());
    }

    @Test
    void read_iso88591_commaDelimiter() throws Exception {
        String sample = "123e4567-e89b-12d3-a456-426614174000,Test,2025-01-01T00:00:00Z\n";
        Files.writeString(tmp, sample, java.nio.charset.StandardCharsets.ISO_8859_1);

        List<ProcessingRequest> out =
            CsvUtil.readRequests(tmp, StandardCharsets.ISO_8859_1, ',');

        assertEquals(1, out.size());
        assertEquals("Test", out.get(0).title());
    }
}
