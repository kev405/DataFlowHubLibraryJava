package com.practice.domain.processing;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.practice.domain.Utils.Enums.RequestStatus;
import com.practice.domain.bacthconfig.BatchJobConfig;
import com.practice.domain.datafile.DataFile;
import com.practice.domain.user.User;

public class ProcessingRequest {

    private final UUID                id;
    private final String              title;
    private final DataFile            dataFile;
    private final Map<String,String>  parameters;      // unmodifiable copy
    private volatile RequestStatus    status;          // only mutable field
    private final Instant             createdAt;
    private final User                requestedBy;
    private final BatchJobConfig      batchJobConfig;

    public ProcessingRequest(UUID id, String title, DataFile dataFile, Map<String,String> parameters, RequestStatus status, Instant createdAt, User requestedBy, BatchJobConfig batchJobConfig) {
        this.id = id;
        this.title = title;
        this.dataFile = dataFile;
        this.parameters = Map.copyOf(parameters);
        this.status = status;
        this.createdAt = createdAt;
        this.requestedBy = requestedBy;
        this.batchJobConfig = batchJobConfig;
    }

    /* ----- getters ----- */
    public UUID getId() {   
        return id;
    }
    public String getTitle() {
        return title;
    }
    public DataFile getDataFile() {
        return dataFile;
    }
    public Map<String, String> getParameters() {
        return parameters;
    }
    public RequestStatus getStatus() {
        return status;
    }
    public Instant getCreatedAt() {
        return createdAt;
    }
    public User getRequestedBy() {
        return requestedBy;
    }
    public BatchJobConfig getBatchJobConfig() {
        return batchJobConfig;
    }
    /* ----- negocio ----- */
    public void updateStatus(RequestStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        this.status = newStatus;
    }
    
}
