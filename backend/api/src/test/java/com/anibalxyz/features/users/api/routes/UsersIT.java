package com.anibalxyz.features.users.api.routes;

import com.anibalxyz.core.api.response.mappers.ErrorMapperResult;
import com.anibalxyz.core.application.ValidationNotification;
import com.anibalxyz.features.users.domain.*;
import com.anibalxyz.features.users.domain.error.EmailAlreadyTakenError;
import com.anibalxyz.features.users.domain.error.UserDomainError;
import com.anibalxyz.features.users.infra.JpaUserRepository;
import com.anibalxyz.server.http.mappers.FeaturesErrorMapper;
import com.anibalxyz.shared.IntegrationTest;
import com.anibalxyz.shared.ResultAsserts;
import org.junit.jupiter.api.*;

public abstract class UsersIT extends IntegrationTest {
  protected UserRepository userRepository;

  protected static ErrorMapperResult errorResultFromAlreadyTakenEmail() {
    ValidationNotification<UserDomainError> correspondentError = new ValidationNotification<>();
    correspondentError.add("email", new EmailAlreadyTakenError());
    return FeaturesErrorMapper.map(correspondentError);
  }

  protected static ErrorMapperResult errorResultFromInvalidName(String name) {
    ValidationNotification<UserDomainError> correspondentError = new ValidationNotification<>();
    correspondentError.add("name", ResultAsserts.failure(Name.validate(name)));
    return FeaturesErrorMapper.map(correspondentError);
  }

  @BeforeEach
  public void deps() {
    userRepository = new JpaUserRepository(() -> em);
  }
}
