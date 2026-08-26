package com.anibalxyz.features.users.infra;

import com.anibalxyz.features.users.domain.*;
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

  @Column(name = "email", nullable = false, unique = true)
  private String email;

  @Column(name = "password_hash", nullable = false)
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
        user.id() == null ? null : user.id().value(),
        user.name().value(),
        user.email().value(),
        user.passwordHash().value(),
        user.createdAt(),
        user.updatedAt());
  }

  public User toDomain() throws CorruptedEmail, CorruptedName, CorruptedPasswordHash {
    // NOTE: some branches will probably not be covered by the current integration tests.
    //        Once unit tests are implemented, then full coverage will be possible
    UserId id = new UserId(this.id);
    Email email = Email.of(this.email).orThrow(err -> new CorruptedEmail(this.email, id.value()));
    Name name = Name.of(this.name).orThrow(err -> new CorruptedName(this.name, id.value()));
    PasswordHash passwordHash =
        PasswordHash.reconstitute(this.passwordHash)
            .orThrow(err -> new CorruptedPasswordHash(this.passwordHash, id.value()));

    return User.reconstitute(id, name, email, passwordHash, createdAt, updatedAt);
  }

  public Integer id() {
    return id;
  }
}
