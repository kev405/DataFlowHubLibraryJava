package com.practice;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.practice.error.ErrorHandler;
import com.practice.exception.DatabaseUnavailableException;
import com.practice.exception.InfraException;
import com.practice.exception.InvalidFileFormatException;
import com.practice.exception.UserNotFoundException;

class ErrorHandlerTest {

    private static final Logger LOG = LoggerFactory.getLogger("test");

    @Test
    void format_includesCodeTypeAndMessage() {
        Throwable ex = new DatabaseUnavailableException("db down");
        String txt = ErrorHandler.format(ex, false);

        assertTrue(txt.startsWith("[DB_DOWN] DatabaseUnavailableException"));
        assertTrue(txt.contains("db down"));
    }

    @Test
    void format_truncatesStackTrace_whenNotVerbose() {
        Throwable ex = new InvalidFileFormatException("bad csv");
        String txt = ErrorHandler.format(ex, false);
        long lines = txt.lines().count();

        assertTrue(lines <= 7, "5 stack lines + header expected");
    }

    @Test
    void rootCausePreserved_inFormat() {
        Throwable root = new RuntimeException("root cause");
        Throwable ex   = new InfraException("wrapper", root);
        String txt = ErrorHandler.format(ex, true);

        assertTrue(txt.contains("root cause"));
    }

    @Test
    void log_doesNotThrow() {
        Throwable ex = new UserNotFoundException("id=X");
        assertDoesNotThrow(() -> ErrorHandler.log(LOG, ex, false));
    }
}
