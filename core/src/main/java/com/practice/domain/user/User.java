package com.practice.domain.user;

import java.util.UUID;
import java.time.Instant;
import java.util.Objects;
import com.practice.domain.Utils.Enums.UserRole;

public final class User {
    
    private final UUID id;
    private final String name;
    private final String email;
    private UserRole role;
    private final Instant createdAt;

    public User(UUID id, String name, String email, UserRole role, Instant createdAt) {
        this.id        = Objects.requireNonNull(id);
        this.name      = requireNonBlank(name);
        this.email     = requireValidEmail(email);
        this.role      = Objects.requireNonNull(role);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    private static String requireValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email must not be blank");
        }
        
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        return email;
    }

    private static String requireNonBlank(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Name must not be blank");
        }
        return value;
    }

    /* ----- negocio ----- */
    public void promoteTo(UserRole newRole) {
        if (newRole.ordinal() < this.role.ordinal())
            throw new IllegalArgumentException("Cannot demote user");
        this.role = newRole;
    }
    
    /* ----- getters ----- */
    public UUID getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public String getEmail() {
        return email;
    }
    public UserRole getRole() {
        return role;
    }
    public Instant getCreatedAt() {
        return createdAt;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return id.equals(user.id);
    }
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role.getRoleName() +
                ", createdAt=" + createdAt +
                '}';

        }
}