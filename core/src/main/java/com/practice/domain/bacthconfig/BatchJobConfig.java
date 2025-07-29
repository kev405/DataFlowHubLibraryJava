package com.practice.domain.bacthconfig;

import java.time.Instant;
import java.util.UUID;

import com.practice.domain.Utils.Enums.ReaderType;
import com.practice.domain.Utils.Enums.WriterType;

public class BatchJobConfig {
    
    private final UUID        id;
    private final String      name;
    private final String      description;
    private final int         chunkSize;
    private final ReaderType  readerType;
    private final WriterType  writerType;
    private final boolean     allowRestart;
    private final Instant     createdAt;
    private final boolean     active;

    public BatchJobConfig(UUID id, String name, String description, int chunkSize, ReaderType readerType, WriterType writerType, boolean allowRestart, Instant createdAt, boolean active) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.chunkSize = chunkSize;
        this.readerType = readerType;
        this.writerType = writerType;
        this.allowRestart = allowRestart;
        this.createdAt = createdAt;
        this.active = active;
    }

    /* ----- getters ----- */
    public UUID getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public String getDescription() {
        return description;
    }
    public int getChunkSize() {
        return chunkSize;
    }
    public ReaderType getReaderType() {
        return readerType;
    }
    public WriterType getWriterType() {
        return writerType;
    }
    public boolean isAllowRestart() {
        return allowRestart;
    }
    public Instant getCreatedAt() {
        return createdAt;
    }
    public boolean isActive() {
        return active;
    }
}
