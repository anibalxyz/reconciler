package com.anibalxyz.features.users.application;

import com.anibalxyz.core.Result;
import com.anibalxyz.features.users.domain.Email;
import com.anibalxyz.features.users.domain.User;
import com.anibalxyz.features.users.domain.UserRepository;
import com.anibalxyz.features.users.domain.error.UserDomainError;
import com.anibalxyz.features.users.domain.error.UserNotFoundError;

public class GetUserByEmail {
  private final UserRepository userRepository;

  public GetUserByEmail(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public Result<User, UserDomainError> execute(String email) {
    return Email.of(email)
        .<UserDomainError>mapError(err -> err)
        .flatMap(
            validEmail ->
                userRepository
                    .findByEmail(validEmail)
                    .map(Result::<User, UserDomainError>success)
                    .orElseGet(() -> Result.failure(UserNotFoundError.byEmail(email))));
  }
}
