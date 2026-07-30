package com.anibalxyz.features.users.domain;

import com.anibalxyz.annotation.ExcludeFromJacocoGenerated;
import java.time.Instant;
import java.util.Objects;

public class User {

  private final Integer id;
  private final Name name;
  private final Email email;
  private final PasswordHash passwordHash;
  private final Instant createdAt;
  private final Instant updatedAt;

  @ExcludeFromJacocoGenerated
  private User(
      Integer id,
      Name name,
      Email email,
      PasswordHash passwordHash,
      Instant createdAt,
      Instant updatedAt) {
    Objects.requireNonNull(name, "name cannot be null");
    Objects.requireNonNull(email, "email cannot be null");
    Objects.requireNonNull(passwordHash, "passwordHash cannot be null");
    this.id = id;
    this.name = name;
    this.email = email;
    this.passwordHash = passwordHash;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  /** Create a new {@code user} that has not yet been persisted. */
  @ExcludeFromJacocoGenerated
  public static User create(Name name, Email email, PasswordHash passwordHash) {

    return new User(null, name, email, passwordHash, null, null);
  }

  @ExcludeFromJacocoGenerated
  public static User reconstitute(
      Integer id,
      Name name,
      Email email,
      PasswordHash passwordHash,
      Instant createdAt,
      Instant updatedAt) {
    Objects.requireNonNull(id, "id cannot be null");
    Objects.requireNonNull(createdAt, "createdAt cannot be null");
    Objects.requireNonNull(updatedAt, "updatedAt cannot be null");
    return new User(id, name, email, passwordHash, createdAt, updatedAt);
  }

  public Integer id() {
    return id;
  }

  public Name name() {
    return name;
  }

  public Email email() {
    return email;
  }

  public PasswordHash passwordHash() {
    return passwordHash;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }

  public boolean passwordMatches(String password) {
    return this.passwordHash.matches(password);
  }

  @ExcludeFromJacocoGenerated
  public User withName(Name name) {
    return new User(id, name, email, passwordHash, createdAt, updatedAt);
  }

  @ExcludeFromJacocoGenerated
  public User withEmail(Email email) {
    return new User(id, name, email, passwordHash, createdAt, updatedAt);
  }

  @ExcludeFromJacocoGenerated
  public User withPasswordHash(PasswordHash passwordHash) {
    return new User(id, name, email, passwordHash, createdAt, updatedAt);
  }

  @Override
  public int hashCode() {
    return id == null ? 0 : Objects.hash(id);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof User other)) return false;
    if (this.id == null || other.id == null) return false;
    return this.id.equals(other.id);
  }

  @ExcludeFromJacocoGenerated
  @Override
  public String toString() {
    return
"""
User(id=%s, name=%s, email=%s, passwordHash=%s, createdAt=%s, updatedAt=%s)"""
        .formatted(id, name.value(), email.value(), passwordHash.toString(), createdAt, updatedAt);
  }
}
