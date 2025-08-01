package com.practice.domain.utils.enums;

public enum RequestStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED;

    public String getStatusName() {
        return this.name();
    }
    
}
