package com.anibalxyz.features.users.domain;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

  List<User> findAll();

  Optional<User> findById(Integer id);

  Optional<User> findByEmail(Email email);

  /**
   * This can be used for both creating a new user and updating an existing one.
   *
   * @return The saved user, which will include a generated ID and timestamps.
   */
  User save(User user);

  /**
   * @return {@code true} if a user was deleted, {@code false} otherwise.
   */
  boolean deleteById(Integer id);
}
