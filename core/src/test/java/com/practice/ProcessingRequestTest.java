package com.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.practice.domain.datafile.DataFile;
import com.practice.domain.processing.ProcessingRequest;
import com.practice.domain.user.User;
import com.practice.domain.utils.enums.RequestStatus;
import com.practice.domain.utils.enums.UserRole;

import nl.jqno.equalsverifier.EqualsVerifier;

/** Contract tests for {@link ProcessingRequest}. */
public class ProcessingRequestTest {

    private ProcessingRequest givenPending() {
        return new ProcessingRequest(
            UUID.randomUUID(), "ETL Job",
            TestFixtures.sampleFile(),
            Map.of("sep", "|"),
            TestFixtures.sampleUser(),
            TestFixtures.sampleConfig(),
            Instant.now());
    }

    @Test
    void pending_toInProgress_toFailed() {
        ProcessingRequest r = givenPending();
        r.markInProgress();
        r.markFailed();
        assertEquals(RequestStatus.FAILED, r.status());
    }

    @Test
    void parameters_areUnmodifiable() {
        ProcessingRequest r = givenPending();
        assertThrows(UnsupportedOperationException.class,
                     () -> r.parameters().put("new", "value"));
    }

    @Test
    void illegalTransition_throws() {
        ProcessingRequest r = givenPending();
        // Skipping IN_PROGRESS state is not allowed
        assertThrows(IllegalStateException.class, r::markCompleted);
    }
    
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

        EqualsVerifier.forClass(ProcessingRequest.class)
            .withNonnullFields("id")
            .withOnlyTheseFields("id")
            .withPrefabValues(DataFile.class, dataFile1, dataFile2)
            .verify();
    }
}
