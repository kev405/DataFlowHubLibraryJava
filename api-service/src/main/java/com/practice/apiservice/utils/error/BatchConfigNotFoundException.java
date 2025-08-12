package com.practice.apiservice.utils.error;

public class BatchConfigNotFoundException extends RuntimeException {
    public BatchConfigNotFoundException(String configId) {
        super("Unknown batch config: " + configId);
    }
}
