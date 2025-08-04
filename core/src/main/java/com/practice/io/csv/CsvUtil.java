package com.practice.io.csv;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.practice.domain.processing.ProcessingRequest;

/**
 * Tiny CSV helper for ProcessingRequest.
 * - No external libs; just java.nio & Streams.
 */
public final class CsvUtil {

    private CsvUtil() { }

    /* --------------------------------------------------------------
       Public API
       -------------------------------------------------------------- */

    public static List<ProcessingRequest> readRequests(
            Path csv, Charset cs, char delimiter) throws Exception {

        try (BufferedReader br = Files.newBufferedReader(csv, cs)) {
            return br.lines()
                     .map(l -> parseLine(l, delimiter))
                     .collect(Collectors.toList());
        }
    }

    public static void writeRequests(
            Path csv, List<ProcessingRequest> list,
            Charset cs, char delimiter) throws Exception {

        try (BufferedWriter bw = Files.newBufferedWriter(
                csv, cs, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

            for (ProcessingRequest pr : list) {
                bw.write(formatLine(pr, delimiter));
                bw.newLine();                       // correct \n on *nix, \r\n on Windows
            }
        }
    }

    /* --------------------------------------------------------------
       Internals (very simple — no quoted fields)
       -------------------------------------------------------------- */

    private static ProcessingRequest parseLine(String l, char d) {
        String[] p = l.split("\\" + d);
        if (p.length < 3) {
            throw new IllegalArgumentException("CSV line must have at least 3 fields: " + l);
        }
        
        // Create stub objects for required non-null fields
        var stubDataFile = new com.practice.domain.datafile.DataFile(
            UUID.randomUUID(), "stub.csv", "/tmp/stub.csv", 1L,
            "0000000000000000000000000000000000000000000000000000000000000000",
            java.time.Instant.now(), createStubUser()
        );
        
        var stubUser = createStubUser();
        var stubConfig = com.practice.domain.batchconfig.BatchJobConfig.builder("STUB").chunkSize(100).build();
        
        return new ProcessingRequest(
                UUID.fromString(p[0]),
                p[1],                                // title
                stubDataFile,                        // dataFile - stub object
                Map.<String,String>of(),             // params - empty map with correct types
                stubUser,                            // requestedBy - stub user
                stubConfig,                          // batchJobConfig - stub config
                java.time.Instant.parse(p[2]));     // createdAt
    }
    
    private static com.practice.domain.user.User createStubUser() {
        return new com.practice.domain.user.User(
            UUID.randomUUID(), "StubUser", "stub@example.com",
            com.practice.domain.utils.enums.UserRole.OPERATOR, java.time.Instant.now()
        );
    }

    private static String formatLine(ProcessingRequest pr, char d) {
        return String.join(String.valueOf(d),
                pr.id().toString(),
                pr.title(),
                pr.createdAt().toString());
    }
}
