package com.practice.domain.utils.enums;

public enum UserRole {
    ADMIN,
    OPERATOR,
    ANALYST;

    public String getRoleName() {
        return this.name();
    }
}
