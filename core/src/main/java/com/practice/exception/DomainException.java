package com.practice.exception;

/**
 * Checked exception used for business-rule violations.
 */
public class DomainException extends DataflowException {

    public DomainException(String message)                     { super(message); }
    public DomainException(String message, Throwable cause)    { super(message, cause); }
}