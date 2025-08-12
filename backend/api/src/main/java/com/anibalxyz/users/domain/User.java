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
    return new User(id, this.name, this.email, this.passwordHash, this.createdAt, this.updatedAt);
  }

  public User withName(String name) {
    return new User(this.id, name, this.email, this.passwordHash, this.createdAt, this.updatedAt);
  }

  public User withEmail(Email email) {
    return new User(this.id, this.name, email, this.passwordHash, this.createdAt, this.updatedAt);
  }

  public User withPasswordHash(PasswordHash passwordHash) {
    return new User(this.id, this.name, this.email, passwordHash, this.createdAt, this.updatedAt);
  }

  public Integer getId() {
    return this.id;
  }

  public String getName() {
    return this.name;
  }

  public Email getEmail() {
    return this.email;
  }

  public PasswordHash getPasswordHash() {
    return this.passwordHash;
  }

  public LocalDateTime getCreatedAt() {
    return this.createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return this.updatedAt;
  }
}
