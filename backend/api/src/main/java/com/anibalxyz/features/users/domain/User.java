package com.anibalxyz.features.users.domain;

import com.anibalxyz.annotation.ExcludeFromJacocoGenerated;
import java.time.Instant;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a User in the domain.
 *
 * <p>This class is the root entity for the User aggregate. It is designed to be immutable; any
 * modification returns a new {@code User} instance with the updated value using the {@code with...}
 * methods. This ensures predictable state management and thread safety.
 */
public record User(
    Integer id,
    Name name,
    Email email,
    PasswordHash passwordHash,
    Instant createdAt,
    Instant updatedAt) {
  @ExcludeFromJacocoGenerated(reason = "SoC violated - will be deleted once removed from OpenAPI ")
  public static final int NAME_MAX_LENGTH = 100;

  /** Constructor for creating a new user that has not yet been persisted. */
  public User(Name name, Email email, PasswordHash passwordHash) {
    this(null, name, email, passwordHash, null, null);
  }

  public User withId(Integer id) {
    return new User(id, name, email, passwordHash, createdAt, updatedAt);
  }

  public User withName(Name name) {
    return new User(id, name, email, passwordHash, createdAt, updatedAt);
  }

  public User withEmail(Email email) {
    return new User(id, name, email, passwordHash, createdAt, updatedAt);
  }

  public User withPasswordHash(PasswordHash passwordHash) {
    return new User(id, name, email, passwordHash, createdAt, updatedAt);
  }

  public User withCreatedAt(Instant createdAt) {
    return new User(id, name, email, passwordHash, createdAt, updatedAt);
  }

  public User withUpdatedAt(Instant updatedAt) {
    return new User(id, name, email, passwordHash, createdAt, updatedAt);
  }

  @Override
  public @NotNull String toString() {
    return
"""
User(id=%s, name=%s, email=%s, passwordHash=%s, createdAt=%s, updatedAt=%s)"""
        .formatted(id, name.value(), email.value(), passwordHash.toString(), createdAt, updatedAt);
  }
}
