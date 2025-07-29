package com.practice.domain.Utils.Enums;

public enum ReaderType {
    CSV,
    JDBC,
    JSON;

    public String getTypeName() {
        return this.name();
    }
}
