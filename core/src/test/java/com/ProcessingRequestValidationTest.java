package com;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.practice.domain.processing.ProcessingRequest;

/** Ensures constructor rejects null arguments. */
class ProcessingRequestValidationTest {

    @Test
    void nullDataFile_throwsNullPointer() {
        assertThrows(NullPointerException.class, () ->
            new ProcessingRequest(
                UUID.randomUUID(), "title",
                null,                        // <-- invalid
                Map.of(), TestFixtures.sampleUser(),
                TestFixtures.sampleConfig(),
                Instant.now()));
    }

    @Test
    void negativeChunkSize_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () ->
            TestFixtures.sampleConfig().builder("bad").chunkSize(0).build());
    }
}
