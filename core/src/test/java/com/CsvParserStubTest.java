package com;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.practice.exception.InvalidFileFormatException;

/**
 * Stub parser used only to verify that a corrupt CSV
 * triggers InvalidFileFormatException.
 */
class CsvParserStubTest {

    static class CsvParser {
        void parse(Path file) throws InvalidFileFormatException {
            // Always fails to simulate corrupt file
            throw new InvalidFileFormatException("Bad CSV header");
        }
    }

    @Test
    void parse_invalidFile_throwsException() {
        CsvParser parser = new CsvParser();
        Path badCsv = Path.of("src/test/resources/bad.csv");

        assertThrows(InvalidFileFormatException.class,
                     () -> parser.parse(badCsv));
    }
}
