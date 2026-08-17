package com.anibalxyz.features.users.api.routes;


import com.anibalxyz.core.application.ValidationNotification;
import com.anibalxyz.features.users.domain.*;
import com.anibalxyz.features.users.domain.error.EmailAlreadyTakenError;
import com.anibalxyz.features.users.domain.error.UserDomainError;
import com.anibalxyz.features.users.infra.JpaUserRepository;
import com.anibalxyz.server.api.ErrorMapper;
import com.anibalxyz.server.api.ErrorResult;
import com.anibalxyz.shared.IntegrationTest;
import com.anibalxyz.shared.ResultAsserts;
import org.junit.jupiter.api.*;

public abstract class BaseUsersIntegrationTest extends IntegrationTest {
  protected UserRepository userRepository;

  protected static ErrorResult errorResultFromAlreadyTakenEmail() {
    ValidationNotification<UserDomainError> correspondentError = new ValidationNotification<>();
    correspondentError.add("email", new EmailAlreadyTakenError());
    return ErrorMapper.map(correspondentError);
  }

  protected static ErrorResult errorResultFromInvalidName(String name) {
    ValidationNotification<UserDomainError> correspondentError = new ValidationNotification<>();
    correspondentError.add("name", ResultAsserts.failure(Name.validate(name)));
    return ErrorMapper.map(correspondentError);
  }

  @BeforeEach
  public void deps() {
    userRepository = new JpaUserRepository(() -> em);
  }
}
