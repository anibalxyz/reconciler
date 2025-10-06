package com.anibalxyz.users.domain;

import java.time.LocalDateTime;

public class User {
  private final Integer id;
  private final String name;
  private final Email email;
  private final PasswordHash passwordHash;
  private final LocalDateTime createdAt;
  private final LocalDateTime updatedAt;

  public User(
      Integer id,
      String name,
      Email email,
      PasswordHash passwordHash,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.name = name;
    this.email = email;
    this.passwordHash = passwordHash;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public User(String name, Email email, PasswordHash passwordHash) {
    this(null, name, email, passwordHash, null, null);
  }

  public User withId(Integer id) {
    return new User(id, name, email, passwordHash, createdAt, updatedAt);
  }

  public User withName(String name) {
    return new User(id, name, email, passwordHash, createdAt, updatedAt);
  }

  public User withEmail(Email email) {
    return new User(id, name, email, passwordHash, createdAt, updatedAt);
  }

  public User withPasswordHash(PasswordHash passwordHash) {
    return new User(id, name, email, passwordHash, createdAt, updatedAt);
  }

  public User withCreatedAt(LocalDateTime createdAt) {
    return new User(id, name, email, passwordHash, createdAt, updatedAt);
  }

  public User withUpdatedAt(LocalDateTime updatedAt) {
    return new User(id, name, email, passwordHash, createdAt, updatedAt);
  }

  public Integer getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public Email getEmail() {
    return email;
  }

  public PasswordHash getPasswordHash() {
    return passwordHash;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  @Override
  public String toString() {
    return
"""
User(id=%s, name=%s, email=%s, passwordHash=%s, createdAt=%s, updatedAt=%s)"""
        .formatted(id, name, email.value(), passwordHash.toString(), createdAt, updatedAt);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    User user = (User) o;

    return java.util.Objects.equals(id, user.id)
        && java.util.Objects.equals(name, user.name)
        && java.util.Objects.equals(email, user.email)
        && java.util.Objects.equals(passwordHash, user.passwordHash)
        && java.util.Objects.equals(createdAt, user.createdAt)
        && java.util.Objects.equals(updatedAt, user.updatedAt);
  }
}
