package com.anibalxyz.features.users.application;

import com.anibalxyz.core.Result;
import com.anibalxyz.core.domain.error.DomainError;
import com.anibalxyz.features.users.domain.Email;
import com.anibalxyz.features.users.domain.User;
import com.anibalxyz.features.users.domain.UserRepository;
import com.anibalxyz.features.users.domain.error.UserNotFoundError;

public class GetUserByEmail {
  private final UserRepository userRepository;

  public GetUserByEmail(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  // TODO: make it a sealed interface
  //       I found it difficult to use, e.g. during tests. Consumer is not able to know the
  //       available errors just by reading the method signature -> it must read the method
  public Result<User, DomainError> execute(String email) {
    return Email.of(email)
        .<DomainError>mapError(err -> err)
        .flatMap(
            validEmail ->
                userRepository
                    .findByEmail(validEmail)
                    .map(Result::<User, DomainError>success)
                    .orElseGet(() -> Result.failure(UserNotFoundError.byEmail(email))));
  }
}
