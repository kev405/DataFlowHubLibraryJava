package com.practice.domain.jobexecution;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.practice.domain.Utils.Enums.ExecutionStatus;
import com.practice.domain.processing.ProcessingRequest;

/**
 * Runtime record of a single execution attempt for a {@link ProcessingRequest}.
 * <p>
 *     Immutable *except* for the fields that are populated once the job finishes
 *     ({@code endTime, exitStatus, readCount, writeCount, skipCount, errorMessage}).
 *     <br>The method {@link #finish} may be called exactly once.
 * </p>
 */

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

    /* ──────── ctor ──────── */
    public JobExecution(UUID id,
                        ProcessingRequest processingRequest,
                        Instant startTime) {
        this.id               = Objects.requireNonNull(id);
        this.processingRequest= Objects.requireNonNull(processingRequest);
        this.startTime        = Objects.requireNonNull(startTime);
    }

    /* ──────── domain behaviour ──────── */

    /**
     * Completes this execution and stores metrics.
     * May be invoked once; subsequent calls throw {@link IllegalStateException}.
     *
     * @param status        final status (SUCCESS, FAIL, STOPPED)
     * @param endTime       instant &gt; startTime
     * @param read          total items read (≥ 0)
     * @param written       total items written (≥ 0)
     * @param skipped       total items skipped (≥ 0)
     * @param errorMessage  optional short stack-trace or description
     */
    public synchronized void finish(ExecutionStatus status,
                                    Instant endTime,
                                    long read,
                                    long written,
                                    long skipped,
                                    String errorMessage) {

        if (this.exitStatus != null) {
            throw new IllegalStateException("JobExecution already finished");
        }
        Objects.requireNonNull(status);
        Objects.requireNonNull(endTime);
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }
        if (read   < 0 || written < 0 || skipped < 0) {
            throw new IllegalArgumentException("metrics cannot be negative");
        }

        this.exitStatus  = status;
        this.endTime     = endTime;
        this.readCount   = read;
        this.writeCount  = written;
        this.skipCount   = skipped;
        this.errorMessage= errorMessage;
    }

    /* ──────── getters ──────── */

    public UUID              id()               { return id; }
    public ProcessingRequest processingRequest(){ return processingRequest; }
    public Instant           startTime()        { return startTime; }
    public Instant           endTime()          { return endTime; }
    public ExecutionStatus   exitStatus()       { return exitStatus; }
    public long              readCount()        { return readCount; }
    public long              writeCount()       { return writeCount; }
    public long              skipCount()        { return skipCount; }
    public String            errorMessage()     { return errorMessage; }
}
