package com.anibalxyz.features.users.application;

import com.anibalxyz.core.Result;
import com.anibalxyz.features.users.domain.UserRepository;
import com.anibalxyz.features.users.domain.error.UserNotFoundError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteUserById {
  private static final Logger log = LoggerFactory.getLogger(DeleteUserById.class);
  private final UserRepository userRepository;

  public DeleteUserById(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /**
   * Deletes a user by their ID.
   *
   * @param id The ID of the user to delete.
   */
  public Result<Void, UserNotFoundError> execute(int id) {
    if (userRepository.deleteById(id)) {
      log.info("User deleted");
      return Result.success();
    }
    return Result.failure(UserNotFoundError.byId(id));
  }
}
