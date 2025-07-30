package com.practice.domain.Utils.Enums;

public enum UserRole {
    ADMIN,
    OPERATOR,
    ANALYST;

    public String getRoleName() {
        return this.name();
    }
}
