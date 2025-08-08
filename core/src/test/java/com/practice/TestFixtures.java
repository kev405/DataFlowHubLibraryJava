package com.practice;

import com.practice.domain.batchconfig.BatchJobConfig;
import com.practice.domain.datafile.DataFile;
import com.practice.domain.jobexecution.JobExecution;
import com.practice.domain.processing.ProcessingRequest;
import com.practice.domain.user.User;
import com.practice.domain.utils.enums.UserRole;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Common factory helpers for unit tests. */
public final class TestFixtures {

    private TestFixtures() { }

    public static User sampleUser() {
        return new User(
            UUID.randomUUID(), "Alice", "alice@example.com",
            UserRole.OPERATOR, Instant.now()
        );
    }

    public static DataFile sampleFile() {
        return new DataFile(
            UUID.randomUUID(), "data.csv", "/tmp/data.csv", 123L,
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
            Instant.now(), sampleUser()
        );
    }

    public static BatchJobConfig sampleConfig() {
        return BatchJobConfig.builder("ETL-Users").chunkSize(500).build();
    }

    public static ProcessingRequest newPendingRequest() {
        return new ProcessingRequest(
            UUID.randomUUID(), "Import", sampleFile(),
            Map.of("sep", ","), sampleUser(), sampleConfig(), Instant.now()
        );
    }

    public static JobExecution newInProgressExecution() {
        // starts in IN_PROGRESS state to simplify tests
        ProcessingRequest req = newPendingRequest();
        req.markInProgress();
        return new JobExecution(UUID.randomUUID(), req, Instant.now());
    }
    
}
