package com.practice.exception;

/** Thrown when the database is down or not reachable. */
public class DatabaseUnavailableException extends InfraException {
    public DatabaseUnavailableException(String msg)               { super(msg); }
    public DatabaseUnavailableException(String msg, Throwable c)  { super(msg, c); }
}
