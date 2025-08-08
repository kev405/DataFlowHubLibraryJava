package com.practice;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.practice.exception.DatabaseUnavailableException;
import com.practice.exception.DataflowException;
import com.practice.exception.DomainException;
import com.practice.exception.InfraException;
import com.practice.exception.InvalidFileFormatException;

/**
 * Verifies inheritance and cause-preservation for the exception hierarchy.
 */
class ExceptionHierarchyTest {

    @Test
    void invalidFileFormat_isInstanceOfDomainAndDataflow() {
        InvalidFileFormatException ex = new InvalidFileFormatException("bad csv");
        assertTrue(ex instanceof DomainException);
        assertTrue(ex instanceof DataflowException);
        assertFalse(InfraException.class.isInstance(ex));
    }

    @Test
    void databaseUnavailable_isUncheckedAndKeepsCause() {
        Throwable cause = new RuntimeException("socket timeout");
        DatabaseUnavailableException ex =
            new DatabaseUnavailableException("db down", cause);

        assertTrue(ex instanceof InfraException);
        assertSame(cause, ex.getCause());
    }
}
