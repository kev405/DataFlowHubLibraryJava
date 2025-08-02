package com.practice.exception;

/** Thrown when a user ID cannot be resolved. */
public class UserNotFoundException extends DomainException {
    public UserNotFoundException(String msg)                      { super(msg); }
    public UserNotFoundException(String msg, Throwable c)         { super(msg, c); }
}
