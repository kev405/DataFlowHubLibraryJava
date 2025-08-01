package com;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.practice.domain.user.User;
import com.practice.domain.utils.enums.UserRole;

import nl.jqno.equalsverifier.EqualsVerifier;

/** Contract tests for {@link User}. */
public class UserTest {

    @Test
    void equalsAndHashCode_areBasedOnlyOnId() {
        EqualsVerifier.forClass(User.class)
            .withNonnullFields("id")          // id must never be null
            .withOnlyTheseFields("id")        // equality == id only
            .verify();
    }

    @Test
    void promoteTo_higherRoleChangesState() {
        // Start with ADMIN (lowest ordinal = highest rank) and promote to ANALYST (highest ordinal)
        User u = new User(
            UUID.randomUUID(), "Bob", "bob@example.com",
            UserRole.ADMIN, Instant.now());
        u.promoteTo(UserRole.ANALYST);
        assertEquals(UserRole.ANALYST, u.role());
    }

    @Test
    void promoteTo_lowerRoleThrows() {
        // Start with ANALYST (highest ordinal) and try to demote to ADMIN (lower ordinal = higher rank)
        User u = new User(
            UUID.randomUUID(), "Ann", "ann@example.com",
            UserRole.ANALYST, Instant.now());
        assertThrows(IllegalArgumentException.class,
                     () -> u.promoteTo(UserRole.ADMIN));
    }

    @Test
    void invalidEmail_isRejected() {
        assertThrows(IllegalArgumentException.class,
                     () -> new User(UUID.randomUUID(), "X", "bad-mail",
                                    UserRole.ANALYST, Instant.now()));
    }
    
}
