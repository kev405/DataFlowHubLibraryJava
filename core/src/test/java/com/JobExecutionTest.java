package com;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.practice.domain.datafile.DataFile;
import com.practice.domain.jobexecution.JobExecution;
import com.practice.domain.user.User;
import com.practice.domain.utils.enums.ExecutionStatus;
import com.practice.domain.utils.enums.UserRole;

import nl.jqno.equalsverifier.EqualsVerifier;

/** Contract tests for {@link JobExecution}. */
public class JobExecutionTest {

    @Test
    void equalsAndHashCode_areBasedOnlyOnId() {
        // Create valid DataFile instances for prefab values
        var dataFile1 = new DataFile(
            UUID.randomUUID(),
            "file1.txt",
            "/storage/file1.txt",
            1024L,
            "a".repeat(64), // Valid 64-character hex string
            Instant.now(),
            new User(UUID.randomUUID(), "user1", "user1@example.com", UserRole.ADMIN, Instant.now())
        );
        
        var dataFile2 = new DataFile(
            UUID.randomUUID(),
            "file2.txt", 
            "/storage/file2.txt",
            2048L,
            "b".repeat(64), // Valid 64-character hex string
            Instant.now(),
            new User(UUID.randomUUID(), "user2", "user2@example.com", UserRole.OPERATOR, Instant.now())
        );

        EqualsVerifier.forClass(JobExecution.class)
            .withNonnullFields("id")          // id must never be null
            .withOnlyTheseFields("id")        // equality == id only
            .withPrefabValues(DataFile.class, dataFile1, dataFile2)
            .suppress(nl.jqno.equalsverifier.Warning.STRICT_INHERITANCE)
            .verify();
    }

    @Test
    void finish_setsMetricsOnce() {
        JobExecution exec = TestFixtures.newInProgressExecution();
        exec.finish(
            ExecutionStatus.SUCCESS,
            Instant.now().plusSeconds(10),
            100, 95, 5, null
        );
        assertEquals(ExecutionStatus.SUCCESS, exec.exitStatus());
        assertEquals(100, exec.readCount());
        // second call must fail
        assertThrows(IllegalStateException.class,
                     () -> exec.finish(ExecutionStatus.FAIL,
                                        Instant.now(), 0,0,0,null));
    }
    
}
