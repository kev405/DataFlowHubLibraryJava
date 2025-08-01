package com.practice.domain.utils.enums;

public enum WriterType {
    JPA,
    FLAT_FILE,
    NO_OP;

    public String getTypeName() {
        return this.name();
    }
}
