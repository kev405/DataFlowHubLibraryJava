package com.practice.domain.utils.enums;

public enum ReaderType {
    CSV,
    JDBC,
    JSON;

    public String getTypeName() {
        return this.name();
    }
}
