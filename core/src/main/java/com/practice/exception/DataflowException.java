package com.practice.exception;

/**
 * Root checked exception for the entire DataFlowHub domain.
 */
public abstract class DataflowException extends Exception {

    protected DataflowException(String message)                { super(message); }
    protected DataflowException(String message, Throwable c)   { super(message, c); }
}
