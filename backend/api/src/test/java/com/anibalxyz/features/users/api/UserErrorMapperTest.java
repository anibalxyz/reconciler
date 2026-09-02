package com.anibalxyz.features.users.api;

import static org.assertj.core.api.Assertions.*;

import com.anibalxyz.core.application.ValidationNotification;
import com.anibalxyz.core.domain.error.DomainError;
import com.anibalxyz.core.domain.error.InvalidValueError;
import com.anibalxyz.features.users.application.UpdateUserById;
import com.anibalxyz.features.users.domain.Email;
import com.anibalxyz.features.users.domain.Name;
import com.anibalxyz.features.users.domain.Password;
import com.anibalxyz.features.users.domain.error.*;
import com.anibalxyz.server.exception.UnhandledErrorException;
import com.anibalxyz.server.exception.UnreachableCodeException;
import com.anibalxyz.shared.ResultAsserts;
import com.anibalxyz.shared.UnitTest;
import org.junit.jupiter.api.*;

@DisplayName("Tests for UserErrorMapper")
public class UserErrorMapperTest extends UnitTest {
  private static final UserNotFoundError userNotFoundError = UserNotFoundError.byId(1);
  private final UserErrorMapper mapper = new UserErrorMapper();

  @Nested
  @DisplayName("mapUserNotFoundError()")
  class MapUserNotFoundError {

    @Test
    @DisplayName("given ById reason, then does not throw any exception")
    public void givenById_doesNotThrow() {
      assertThatCode(() -> mapper.mapUserNotFoundError(userNotFoundError))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("given ByEmail reason, then throw UnreachableCodeException")
    public void givenByEmail_throwUnreachableCodeException() {
      assertThatThrownBy(
              () -> mapper.mapUserNotFoundError(UserNotFoundError.byEmail("user@mail.com")))
          .isInstanceOf(UnreachableCodeException.class);
    }
  }

  @Nested
  @DisplayName("mapUpdateUserByIdError()")
  class MapUpdateUserByIdError {

    @Test
    @DisplayName("given EmptyCommand, then does not throw any exception")
    public void givenEmptyCommand_doesNotThrow() {
      assertThatCode(() -> mapper.mapUpdateUserByIdError(new UpdateUserById.Error.EmptyCommand()))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("given NotFound, then does not throw any exception")
    public void givenNotFound_doesNotThrow() {

      assertThatCode(
              () ->
                  mapper.mapUpdateUserByIdError(
                      new UpdateUserById.Error.NotFound(userNotFoundError)))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("given ValidationFailed, then does not throw any exception")
    public void givenValidationFailed_doesNotThrow() {
      ValidationNotification<UserDomainError> notification = new ValidationNotification<>();
      notification.add("name", InvalidNameError.blank());

      assertThatCode(
              () ->
                  mapper.mapUpdateUserByIdError(
                      new UpdateUserById.Error.ValidationFailed(notification)))
          .doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("supports()")
  class Supports {

    @Test
    @DisplayName("given a UserDomainError, then return true")
    public void givenUserDomainError_returnTrue() {
      assertThat(mapper.supports(userNotFoundError)).isTrue();
    }

    @Test
    @DisplayName("given an Error, then return true")
    public void givenUpdateUserByIdError_returnTrue() {
      assertThat(mapper.supports(new UpdateUserById.Error.EmptyCommand())).isTrue();
    }

    @Test
    @DisplayName("given an unrelated error, then return false")
    public void givenUnrelatedError_returnFalse() {
      assertThat(mapper.supports(new RuntimeException())).isFalse();
    }
  }

  @Nested
  @DisplayName("map()")
  class Map {

    @Test
    @DisplayName("given an Error, then does not throw any exception")
    public void givenUpdateUserByIdError_doesNotThrow() {
      assertThatCode(() -> mapper.map(new UpdateUserById.Error.EmptyCommand()))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("given a UserNotFoundError, then does not throw any exception")
    public void givenUserNotFoundError_doesNotThrow() {
      assertThatCode(() -> mapper.map(userNotFoundError)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("given an unhandled error type, then throw UnhandledErrorException")
    public void givenUnhandledError_throwUnhandledErrorException() {
      assertThatThrownBy(() -> mapper.map(new RuntimeException()))
          .isInstanceOf(UnhandledErrorException.class);
    }
  }

  @Nested
  @DisplayName("supportsFieldError()")
  class SupportsFieldError {

    @Test
    @DisplayName("given a UserDomainError.InvalidValueError, then return true")
    public void givenUserInvalidValueError_returnTrue() {
      assertThat(mapper.supportsFieldError(InvalidEmailError.blank())).isTrue();
    }

    @Test
    @DisplayName("given an unrelated InvalidValueError, then return false")
    public void givenUnrelatedInvalidValueError_returnFalse() {
      assertThat(mapper.supportsFieldError(new InvalidValueError() {})).isFalse();
    }
  }

  @Nested
  @DisplayName("mapFieldError()")
  class MapFieldError {

    @Test
    @DisplayName("given EmailAlreadyTakenError, then does not throw any exception")
    public void givenEmailAlreadyTakenError_doesNotThrow() {
      assertThatCode(() -> mapper.mapFieldError(new EmailAlreadyTakenError()))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("given UserNotFoundError, then throw UnreachableCodeException")
    public void givenUserNotFoundError_throwUnreachableCodeException() {
      assertThatThrownBy(() -> mapper.mapFieldError(userNotFoundError))
          .isInstanceOf(UnreachableCodeException.class);
    }

    @Test
    @DisplayName("given an unhandled DomainError, then throw UnhandledErrorException")
    public void givenUnhandledDomainError_throwUnhandledErrorException() {
      assertThatThrownBy(() -> mapper.mapFieldError(new DomainError() {}))
          .isInstanceOf(UnhandledErrorException.class);
    }
  }

  @Nested
  @DisplayName("mapInvalidValue()")
  class MapInvalidValue {

    @Test
    @DisplayName("given InvalidNameError Blank, then does not throw any exception")
    public void givenInvalidNameBlank_doesNotThrow() {
      assertThatCode(() -> mapper.mapInvalidValue(InvalidNameError.blank()))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("given InvalidNameError Absent, then does not throw any exception")
    public void givenInvalidNameAbsent_doesNotThrow() {
      assertThatCode(() -> mapper.mapInvalidValue(InvalidNameError.absent()))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("given InvalidNameError TooLong, then does not throw any exception")
    public void givenInvalidNameTooLong_doesNotThrow() {
      assertThatCode(
              () ->
                  mapper.mapInvalidValue(
                      ResultAsserts.failure(Name.of("n".repeat(Name.MAX_LENGTH + 1)))))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("given InvalidEmailError Blank, then does not throw any exception")
    public void givenInvalidEmailBlank_doesNotThrow() {
      assertThatCode(() -> mapper.mapInvalidValue(InvalidEmailError.blank()))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("given InvalidEmailError Absent, then does not throw any exception")
    public void givenInvalidEmailAbsent_doesNotThrow() {
      assertThatCode(() -> mapper.mapInvalidValue(InvalidEmailError.absent()))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("given InvalidEmailError InvalidFormat, then does not throw any exception")
    public void givenInvalidEmailFormat_doesNotThrow() {
      assertThatCode(() -> mapper.mapInvalidValue(InvalidEmailError.invalidFormat()))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("given InvalidEmailError TooLong, then does not throw any exception")
    public void givenInvalidEmailTooLong_doesNotThrow() {
      assertThatCode(
              () ->
                  mapper.mapInvalidValue(
                      ResultAsserts.failure(Email.of("e".repeat(Email.MAX_LENGTH + 1)))))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("given InvalidPasswordError Blank, then does not throw any exception")
    public void givenInvalidPasswordBlank_doesNotThrow() {
      assertThatCode(() -> mapper.mapInvalidValue(InvalidPasswordError.blank()))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("given InvalidPasswordError Absent, then does not throw any exception")
    public void givenInvalidPasswordAbsent_doesNotThrow() {
      assertThatCode(() -> mapper.mapInvalidValue(InvalidPasswordError.absent()))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("given InvalidPasswordError TooShort, then does not throw any exception")
    public void givenInvalidPasswordTooShort_doesNotThrow() {
      assertThatCode(() -> mapper.mapInvalidValue(ResultAsserts.failure(Password.validate("a"))))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("given InvalidPasswordError TooLong, then does not throw any exception")
    public void givenInvalidPasswordTooLong_doesNotThrow() {
      assertThatCode(
              () ->
                  mapper.mapInvalidValue(
                      ResultAsserts.failure(
                          Password.validate("p".repeat(Password.MAX_LENGTH + 1)))))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("given an unhandled InvalidValueError, then throw UnhandledErrorException")
    public void givenUnhandledInvalidValueError_throwUnhandledErrorException() {
      assertThatThrownBy(() -> mapper.mapInvalidValue(new InvalidValueError() {}))
          .isInstanceOf(UnhandledErrorException.class);
    }

    @Test
    @DisplayName("given InvalidPasswordHashError, then throw UnreachableCodeException")
    public void givenInvalidPasswordHashError_throwUnreachableException() {
      assertThatThrownBy(() -> mapper.mapInvalidValue(new InvalidPasswordHashError()))
          .isInstanceOf(UnreachableCodeException.class);
    }

    @Test
    @DisplayName("given InvalidUserIdError, then throw UnreachableCodeException")
    public void givenInvalidUserIdError_throwUnreachableCodeException() {
      assertThatThrownBy(() -> mapper.mapInvalidValue(new InvalidUserIdError()))
          .isInstanceOf(UnreachableCodeException.class);
    }
  }
}
