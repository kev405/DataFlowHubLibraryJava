package com.practice.domain.batchconfig;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.practice.domain.utils.enums.ReaderType;
import com.practice.domain.utils.enums.WriterType;

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

    /* ---------- ctor hidden behind Builder ---------- */
    private BatchJobConfig(Builder b) {
        this.id           = b.id;
        this.name         = b.name;
        this.description  = b.description;
        this.chunkSize    = b.chunkSize;
        this.readerType   = b.readerType;
        this.writerType   = b.writerType;
        this.allowRestart = b.allowRestart;
        this.createdAt    = b.createdAt;
        this.active       = b.active;
    }

    /* ---------- builder ---------- */
    public static Builder builder(String logicalName) {
        return new Builder(logicalName);
    }

    public static final class Builder {
        /* required */
        private final UUID   id   = UUID.randomUUID();
        private final String name;
        private Instant      createdAt = Instant.now();

        /* optional + defaults */
        private String     description  = "";
        private int        chunkSize    = 1000;
        private ReaderType readerType   = ReaderType.CSV;
        private WriterType writerType   = WriterType.NO_OP;
        private boolean    allowRestart = false;
        private boolean    active       = true;

        private Builder(String logicalName) {
            this.name = requireNonBlank(logicalName);
        }

        public Builder description(String d)   { this.description = d; return this; }
        public Builder chunkSize(int size)     { this.chunkSize = size; return this; }
        public Builder readerType(ReaderType r){ this.readerType = r;   return this; }
        public Builder writerType(WriterType w){ this.writerType = w;   return this; }
        public Builder allowRestart()          { this.allowRestart = true; return this; }
        public Builder inactive()              { this.active = false;     return this; }
        public Builder createdAt(Instant t)    { this.createdAt = t;      return this; }

        public BatchJobConfig build() {
            if (chunkSize <= 0) {
                throw new IllegalArgumentException("chunkSize must be > 0");
            }
            Objects.requireNonNull(readerType);
            Objects.requireNonNull(writerType);
            Objects.requireNonNull(createdAt);
            return new BatchJobConfig(this);
        }

        private static String requireNonBlank(String s) {
            if (s == null || s.isBlank())
                throw new IllegalArgumentException("name is blank");
            return s;
        }
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
