package com.anibalxyz.features.users.infra;

import com.anibalxyz.features.common.Result;
import com.anibalxyz.features.users.domain.Email;
import com.anibalxyz.features.users.domain.Name;
import com.anibalxyz.features.users.domain.PasswordHash;
import com.anibalxyz.features.users.domain.User;
import com.anibalxyz.features.users.domain.error.InvalidPasswordHashError;
import com.anibalxyz.features.users.infra.exception.CorruptedEmail;
import com.anibalxyz.features.users.infra.exception.CorruptedName;
import com.anibalxyz.features.users.infra.exception.CorruptedPasswordHash;
import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.generator.EventType;

/** JPA entity representing a {@link User}, mapped to the "users" database table. */
@Entity
@Table(name = "users")
public class UserEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Integer id;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Column(name = "email", nullable = false, unique = true, length = 255)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @CurrentTimestamp(event = EventType.INSERT)
  @Column(name = "created_at")
  private Instant createdAt;

  @CurrentTimestamp
  @Column(name = "updated_at")
  private Instant updatedAt;

  protected UserEntity() {}

  public UserEntity(
      Integer id,
      String name,
      String email,
      String passwordHash,
      Instant createdAt,
      Instant updatedAt) {
    this.id = id;
    this.name = name;
    this.email = email;
    this.passwordHash = passwordHash;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public static UserEntity fromDomain(User user) {
    return new UserEntity(
        user.id(),
        user.name().value(),
        user.email().value(),
        user.passwordHash().value(),
        user.createdAt(),
        user.updatedAt());
  }

  public User toDomain() throws CorruptedEmail, CorruptedName, CorruptedPasswordHash {
    // NOTE: some branches will probably not be covered by the current acceptance tests.
    //        Once unit tests are implemented, then full coverage will be possible
    // NOTE: these validations are causing over 7s of delay in the current test suite (Apr 25, 2026)
    Result<Email, ?> emailResult = Email.of(email);
    if (emailResult.isFailure()) throw new CorruptedEmail(email, id);
    Result<Name, ?> nameResult = Name.of(name);
    if (nameResult.isFailure()) throw new CorruptedName(name, id);
    Result<PasswordHash, InvalidPasswordHashError> passwordHashResult =
        PasswordHash.of(passwordHash);
    if (passwordHashResult.isFailure()) throw new CorruptedPasswordHash(passwordHash, id);

    return new User(
        id,
        nameResult.getValue(),
        emailResult.getValue(),
        passwordHashResult.getValue(),
        createdAt,
        updatedAt);
  }
}
