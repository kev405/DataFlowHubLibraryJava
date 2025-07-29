package com.practice.domain.jobexecution;

import java.time.Instant;
import java.util.UUID;

import com.practice.domain.Utils.Enums.ExecutionStatus;
import com.practice.domain.processing.ProcessingRequest;

public class JobExecution {
    
    /* ──────── immutable core ──────── */
    private final UUID              id;
    private final ProcessingRequest processingRequest;
    private final Instant           startTime;

    /* ──────── mutable after finish() ──────── */
    private volatile Instant         endTime;        // nullable until finished
    private volatile ExecutionStatus exitStatus;     // idem
    private volatile long            readCount;
    private volatile long            writeCount;
    private volatile long            skipCount;
    private volatile String          errorMessage;   // nullable

    public JobExecution(UUID id, ProcessingRequest processingRequest, Instant startTime) {
        this.id = id;
        this.processingRequest = processingRequest;
        this.startTime = startTime;
        this.exitStatus = ExecutionStatus.SUCCESS; // default status
    }

    /* ──────── getters ──────── */
    public UUID id() { 
        return id; 
    }
    public ProcessingRequest processingRequest() { 
        return processingRequest; 
    }
    public Instant startTime() { 
        return startTime; 
    }
    public Instant endTime() { 
        return endTime; 
    }
    public ExecutionStatus exitStatus() { 
        return exitStatus; 
    }
    public long readCount() { 
        return readCount; 
    }
    public long writeCount() { 
        return writeCount; 
    }
    public long skipCount() { 
        return skipCount; 
    }
    public String errorMessage() { 
        return errorMessage; 
    }


}
