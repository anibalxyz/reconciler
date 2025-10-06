package com.anibalxyz.users.domain;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
  List<User> findAll();

  Optional<User> findById(Integer id);

  Optional<User> findByEmail(Email email);

  User save(User user) throws IllegalArgumentException;

  boolean deleteById(Integer id);
}
