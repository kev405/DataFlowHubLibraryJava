package com.practice.domain.utils.enums;

public enum ExecutionStatus {
    SUCCESS,
    FAIL,
    STOPPED;

    public String getStatusName() {
        return this.name();
    }
}
