package com.practice;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.practice.domain.datafile.DataFile;

public class DataFileValidationTest {
    
    @Test
    void constructor_rejectsInvalidChecksum() {
        assertThrows(IllegalArgumentException.class, () ->
            new DataFile(
                UUID.randomUUID(), "foo.txt", "/p", 10L,
                "notHex", Instant.now(), TestFixtures.sampleUser()));
    }

}
