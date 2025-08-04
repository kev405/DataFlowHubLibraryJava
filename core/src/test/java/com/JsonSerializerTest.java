package com;

import org.junit.jupiter.api.Test;

import com.practice.domain.batchconfig.BatchJobConfig;
import com.practice.domain.processing.ProcessingRequest;
import com.practice.domain.user.User;
import com.practice.domain.utils.enums.UserRole;
import com.practice.io.JsonSerializer;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JsonSerializerTest {

    @Test
    void user_roundTrip_equals() {
        User u = new User(UUID.randomUUID(), "Kevin",
                          "kevin@univalle.edu", UserRole.ANALYST,
                          Instant.parse("2025-01-01T12:00:00Z"));

        String json = JsonSerializer.toJson(u);
        User copy   = JsonSerializer.fromJson(json, User.class);

        assertEquals(u, copy);
    }

    @Test
    void processingRequest_roundTrip_equals() {
        ProcessingRequest pr = TestFixtures.newPendingRequest();
        String json = JsonSerializer.toJson(pr);
        ProcessingRequest copy =
                JsonSerializer.fromJson(json, ProcessingRequest.class);

        assertEquals(pr, copy);
    }

    @Test
    void deserialise_withUnknownField_ignored() {
        String json = """
        {
          "id"      : "%s",
          "name"    : "Extra KPI",
          "unknown" : "will be ignored"
        }""".formatted(UUID.randomUUID());
        BatchJobConfig cfg = JsonSerializer.fromJson(json, BatchJobConfig.class);
        assertEquals("Extra KPI", cfg.name());
    }

    @Test
    void firstPresent_handlesNullsInCollection() {
        // Use Arrays.asList instead of List.of since List.of doesn't allow nulls
        String json = JsonSerializer.toJson(Arrays.asList("A", null, "B"));
        List<?> list = JsonSerializer.fromJson(json, List.class);
        assertEquals(3, list.size());
        assertNull(list.get(1));
    }
}
