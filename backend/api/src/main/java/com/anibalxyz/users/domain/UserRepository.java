package com.anibalxyz.users.domain;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
  Optional<User> findById(Integer id);

  List<User> findAll();

  User save(User user);

  void deleteById(Integer id);
}
