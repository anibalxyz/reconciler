package com.anibalxyz.features.users.application;

import com.anibalxyz.features.users.domain.User;
import com.anibalxyz.features.users.domain.UserRepository;
import java.util.List;

public class GetAllUsers {
  private final UserRepository userRepository;

  public GetAllUsers(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public List<User> execute() {
    return userRepository.findAll();
  }
}
