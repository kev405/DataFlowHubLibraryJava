package com.practice.domain.processing;

import com.practice.domain.batchconfig.BatchJobConfig;
import com.practice.domain.datafile.DataFile;
import com.practice.domain.user.User;

import java.time.Instant;
import java.util.*;

/**
 * Logical order to execute a file-processing job.
 * <p>
 * Immutable in every field except {@code status}.  
 * State transitions allowed:
 * <pre>
 *      PENDING ──▶ RUNNING ──▶ COMPLETED
 *               └────────────▶ FAILED
 * </pre>
 * Any other transition triggers {@link IllegalStateException}.
 * </p>
 */
public final class ProcessingRequest {

    /* ──────────────── shared enum ──────────────── */
    public enum RequestStatus { PENDING, RUNNING, COMPLETED, FAILED }

    /* ──────────────── fields (immutable) ──────────────── */
    private final UUID                id;
    private final String              title;
    private final DataFile            dataFile;
    private final Map<String,String>  parameters;      // unmodifiable copy
    private volatile RequestStatus    status;          // only mutable field
    private final Instant             createdAt;
    private final User                requestedBy;
    private final BatchJobConfig      batchJobConfig;

    /* ──────────────── ctor ──────────────── */
    public ProcessingRequest(
            UUID id,
            String title,
            DataFile dataFile,
            Map<String,String> parameters,
            User requestedBy,
            BatchJobConfig batchJobConfig,
            Instant createdAt
    ) {
        this.id             = Objects.requireNonNull(id);
        this.title          = requireNonBlank(title);
        this.dataFile       = Objects.requireNonNull(dataFile);
        this.parameters     = Map.copyOf(Objects.requireNonNull(parameters));
        this.status         = RequestStatus.PENDING;
        this.requestedBy    = Objects.requireNonNull(requestedBy);
        this.batchJobConfig = Objects.requireNonNull(batchJobConfig);
        this.createdAt      = Objects.requireNonNull(createdAt);
    }

    /* ──────────────── business methods ──────────────── */

    /** Moves the request from PENDING → RUNNING. */
    public synchronized void markRunning() {
        transition(RequestStatus.PENDING, RequestStatus.RUNNING);
    }

    /** Moves the request from RUNNING → COMPLETED. */
    public synchronized void markCompleted() {
        transition(RequestStatus.RUNNING, RequestStatus.COMPLETED);
    }

    /** Moves the request from RUNNING → FAILED. */
    public synchronized void markFailed() {
        transition(RequestStatus.RUNNING, RequestStatus.FAILED);
    }

    private void transition(RequestStatus expected, RequestStatus target) {
        if (this.status != expected) {
            throw new IllegalStateException(
                "Cannot move from " + status + " to " + target);
        }
        this.status = target;
    }

    /* ──────────────── getters ──────────────── */

    public UUID            id()             { return id; }
    public String          title()          { return title; }
    public DataFile        dataFile()       { return dataFile; }
    public Map<String,String> parameters()  { return parameters; }
    public RequestStatus   status()         { return status; }
    public Instant         createdAt()      { return createdAt; }
    public User            requestedBy()    { return requestedBy; }
    public BatchJobConfig  batchJobConfig() { return batchJobConfig; }

    /* ──────────────── helpers ──────────────── */

    private static String requireNonBlank(String s) {
        if (s == null || s.isBlank())
            throw new IllegalArgumentException("title is blank");
        return s;
    }
}
