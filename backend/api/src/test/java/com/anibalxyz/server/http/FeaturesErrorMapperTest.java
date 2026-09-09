package com.anibalxyz.server.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.anibalxyz.core.api.response.mappers.ErrorMapperResult;
import com.anibalxyz.core.application.ValidationNotification;
import com.anibalxyz.core.domain.DomainError;
import com.anibalxyz.core.domain.InvalidValueError;
import com.anibalxyz.features.users.domain.error.InvalidEmailError;
import com.anibalxyz.features.users.domain.error.UserNotFoundError;
import com.anibalxyz.server.http.mappers.FeaturesErrorMapper;
import com.anibalxyz.shared.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Tests for FeaturesErrorMapper")
public class FeaturesErrorMapperTest extends UnitTest {

  @Test
  @DisplayName("map: given an error with a registered mapper, then return a mapped result")
  public void map_registeredError_returnMappedResult() {
    ErrorMapperResult result = FeaturesErrorMapper.map(UserNotFoundError.byId(1));
    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName(
      "map: given an error with no registered mapper, then throw UnregisteredMapperException")
  public void map_unregisteredError_throwUnregisteredMapperException() {
    assertThatThrownBy(() -> FeaturesErrorMapper.map(new RuntimeException()))
        .isInstanceOf(FeaturesErrorMapper.UnregisteredMapperException.class);
  }

  @Test
  @DisplayName(
      "map: given a ValidationNotification with a registered InvalidValueError, then return a mapped result")
  public void map_validationNotificationWithRegisteredInvalidValueError_returnMappedResult() {
    ValidationNotification<DomainError> notification = new ValidationNotification<>();
    notification.add("email", InvalidEmailError.blank());

    ErrorMapperResult result = FeaturesErrorMapper.map(notification);
    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName(
      "map: given a ValidationNotification with an unregistered InvalidValueError, then throw UnregisteredMapperException")
  public void
      map_validationNotificationWithUnregisteredInvalidValueError_throwUnregisteredMapperException() {
    ValidationNotification<DomainError> notification = new ValidationNotification<>();
    notification.add("field", new InvalidValueError() {});

    assertThatThrownBy(() -> FeaturesErrorMapper.map(notification))
        .isInstanceOf(FeaturesErrorMapper.UnregisteredMapperException.class);
  }
}
