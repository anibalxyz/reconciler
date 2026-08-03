package com.anibalxyz.features.users.application;

import com.anibalxyz.core.Result;
import com.anibalxyz.features.users.domain.User;
import com.anibalxyz.features.users.domain.UserRepository;
import com.anibalxyz.features.users.domain.error.UserNotFoundError;

public class GetUserById {
  private final UserRepository userRepository;

  public GetUserById(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public Result<User, UserNotFoundError> execute(int id) {
    return userRepository
        .findById(id)
        .<Result<User, UserNotFoundError>>map(Result::success)
        .orElseGet(() -> Result.failure(UserNotFoundError.byId(id)));
  }
}
