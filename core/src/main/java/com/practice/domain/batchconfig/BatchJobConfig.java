package com.practice.domain.batchconfig;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.practice.domain.Utils.Enums.ReaderType;
import com.practice.domain.Utils.Enums.WriterType;

/**
 * In-memory definition of a Spring-Batch job template.
 * <p>Immutable; use {@link #builder(String)} to create instances.</p>
 */
public final class BatchJobConfig {

    /* ---------- state ---------- */
    private final UUID        id;
    private final String      name;
    private final String      description;
    private final int         chunkSize;
    private final ReaderType  readerType;
    private final WriterType  writerType;
    private final boolean     allowRestart;
    private final Instant     createdAt;
    private final boolean     active;

    /* ---------- ctor ---------- */
    private BatchJobConfig(UUID id, String name, String description, int chunkSize,
                           ReaderType readerType, WriterType writerType,
                           boolean allowRestart, Instant createdAt, boolean active) {
        this.id           = id;
        this.name         = name;
        this.description  = description;
        this.chunkSize    = chunkSize;
        this.readerType   = readerType;
        this.writerType   = writerType;
        this.allowRestart = allowRestart;
        this.createdAt    = createdAt;
        this.active       = active;
    }


    /* ---------- getters ---------- */
    public UUID       id()           { return id; }
    public String     name()         { return name; }
    public String     description()  { return description; }
    public int        chunkSize()    { return chunkSize; }
    public ReaderType readerType()   { return readerType; }
    public WriterType writerType()   { return writerType; }
    public boolean    allowRestart() { return allowRestart; }
    public Instant    createdAt()    { return createdAt; }
    public boolean    isActive()     { return active; }

    /* ---------- contracts ---------- */
    @Override public boolean equals(Object o) {
        return (this == o) || (o instanceof BatchJobConfig cfg && id.equals(cfg.id));
    }
    @Override public int hashCode() { return id.hashCode(); }
    @Override public String toString() {
        return "BatchJobConfig[" + name + ", id=" + id + "]";
    }
    
}
