package com.anibalxyz.server.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.anibalxyz.core.application.ValidationNotification;
import com.anibalxyz.core.domain.error.DomainError;
import com.anibalxyz.core.domain.error.InvalidValueError;
import com.anibalxyz.features.users.domain.error.InvalidEmailError;
import com.anibalxyz.features.users.domain.error.UserNotFoundError;
import com.anibalxyz.server.exception.UnregisteredMapperException;
import com.anibalxyz.shared.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Tests for ErrorMapper")
public class ErrorMapperTest extends UnitTest {

  @Test
  @DisplayName("map: given an error with a registered mapper, then return a mapped result")
  public void map_registeredError_returnMappedResult() {
    ErrorResult result = ErrorMapper.map(UserNotFoundError.byId(1));
    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName(
      "map: given an error with no registered mapper, then throw UnregisteredMapperException")
  public void map_unregisteredError_throwUnregisteredMapperException() {
    assertThatThrownBy(() -> ErrorMapper.map(new RuntimeException()))
        .isInstanceOf(UnregisteredMapperException.class);
  }

  @Test
  @DisplayName(
      "map: given a ValidationNotification with a registered InvalidValueError, then return a mapped result")
  public void map_validationNotificationWithRegisteredInvalidValueError_returnMappedResult() {
    ValidationNotification<DomainError> notification = new ValidationNotification<>();
    notification.add("email", InvalidEmailError.blank());

    ErrorResult result = ErrorMapper.map(notification);
    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName(
      "map: given a ValidationNotification with an unregistered InvalidValueError, then throw UnregisteredMapperException")
  public void
      map_validationNotificationWithUnregisteredInvalidValueError_throwUnregisteredMapperException() {
    ValidationNotification<DomainError> notification = new ValidationNotification<>();
    notification.add("field", new InvalidValueError() {});

    assertThatThrownBy(() -> ErrorMapper.map(notification))
        .isInstanceOf(UnregisteredMapperException.class);
  }
}
