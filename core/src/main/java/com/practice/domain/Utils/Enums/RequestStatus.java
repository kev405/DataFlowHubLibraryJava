package com.practice.domain.Utils.Enums;

public enum RequestStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED;

    public String getStatusName() {
        return this.name();
    }
    
}
