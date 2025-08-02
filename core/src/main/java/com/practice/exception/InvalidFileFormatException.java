package com.practice.exception;

/** Thrown when an input file does not match the expected format. */
public class InvalidFileFormatException extends DomainException {
    public InvalidFileFormatException(String msg)                { super(msg); }
    public InvalidFileFormatException(String msg, Throwable c)   { super(msg, c); }
}
