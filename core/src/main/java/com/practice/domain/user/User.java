package com.practice.domain.user;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.practice.domain.Utils.Enums.UserRole;

/**
 * Represents an operator of the platform.
 * <p>
 * Every field is immutable except {@code role}, so the user can be
 * promoted over time without changing its identity.
 * </p>
 */
public final class User {

    /* ---------- attributes ---------- */
    private final UUID     id;
    private final String   name;
    private final String   email;
    private       UserRole role;
    private final Instant  createdAt;

    /* ---------- constructor ---------- */
    public User(UUID id,
                String name,
                String email,
                UserRole role,
                Instant createdAt) {

        this.id        = Objects.requireNonNull(id);
        this.name      = requireNonBlank(name, "name");
        this.email     = requireValidEmail(email);
        this.role      = Objects.requireNonNull(role);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    /* ---------- business logic ---------- */

    /**
     * Promotes the user to a higher role.
     *
     * @param newRole the desired role; must be equal or higher than the
     *                current one, otherwise an exception is thrown
     */
    public synchronized void promoteTo(UserRole newRole) {
        Objects.requireNonNull(newRole, "newRole");
        if (newRole.ordinal() < this.role.ordinal()) {
            throw new IllegalArgumentException(
                    "Cannot demote user from " + role + " to " + newRole);
        }
        this.role = newRole;
    }

    /* ---------- getters ---------- */
    public UUID     id()        { return id; }
    public String   name()      { return name; }
    public String   email()     { return email; }
    public UserRole role()      { return role; }
    public Instant  createdAt() { return createdAt; }
    /* ---------- validation helpers ---------- */
    private static String requireNonBlank(String s, String field) {
        if (s == null || s.isBlank())
            throw new IllegalArgumentException(field + " is blank");
        return s;
    }
    private static String requireValidEmail(String mail) {
        requireNonBlank(mail, "email");
        if (!mail.matches("^[\\w.+-]+@[\\w.-]+\\.[\\w]{2,}$")) {
            throw new IllegalArgumentException("Invalid email: " + mail);
        }
        return mail;
    }
}
