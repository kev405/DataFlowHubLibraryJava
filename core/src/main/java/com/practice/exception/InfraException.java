package com.practice.exception;

/**
 * Unchecked exception for infrastructure failures (IO, DB, network).
 */
public class InfraException extends RuntimeException {

    public InfraException(String message)                      { super(message); }
    public InfraException(String message, Throwable cause)     { super(message, cause); }
    
}