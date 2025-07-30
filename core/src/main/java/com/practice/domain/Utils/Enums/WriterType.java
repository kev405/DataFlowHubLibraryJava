package com.practice.domain.Utils.Enums;

public enum WriterType {
    JPA,
    FLAT_FILE,
    NO_OP;

    public String getTypeName() {
        return this.name();
    }
}
