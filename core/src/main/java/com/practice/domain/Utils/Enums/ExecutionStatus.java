package com.practice.domain.Utils.Enums;

public enum ExecutionStatus {
    SUCCESS,
    FAIL,
    STOPPED;

    public String getStatusName() {
        return this.name();
    }
}
