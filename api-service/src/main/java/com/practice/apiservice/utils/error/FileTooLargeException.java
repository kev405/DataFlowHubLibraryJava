package com.practice.apiservice.utils.error;

public class FileTooLargeException extends RuntimeException {
    private final long max;
    private final long actual;

    public FileTooLargeException(long max, long actual) {
        super("FILE_TOO_LARGE");
        this.max = max;
        this.actual = actual;
    }

    public long getMax()   { return max; }
    public long getActual(){ return actual; }
}
