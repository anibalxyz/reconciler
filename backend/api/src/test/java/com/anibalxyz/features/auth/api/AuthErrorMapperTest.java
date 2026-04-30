package com.anibalxyz.features.auth.api;

import static org.assertj.core.api.Assertions.*;

import com.anibalxyz.features.auth.application.AuthService;
import com.anibalxyz.features.auth.application.JwtService;
import com.anibalxyz.features.auth.domain.error.AuthDomainError;
import com.anibalxyz.features.auth.domain.error.InvalidCredentialsError;
import com.anibalxyz.features.auth.domain.error.InvalidRefreshTokenError;
import com.anibalxyz.features.common.api.out.code.CommonErrorCode;
import com.anibalxyz.features.common.api.out.response.error.ErrorResponse;
import com.anibalxyz.features.common.application.ValidationNotification;
import com.anibalxyz.features.common.domain.error.DomainError;
import com.anibalxyz.features.common.domain.error.InvalidValueError;
import com.anibalxyz.features.users.domain.error.UserDomainError;
import com.anibalxyz.server.api.ErrorResult;
import com.anibalxyz.server.exception.UnhandledErrorException;
import com.anibalxyz.server.exception.UnreachableCodeException;
import org.junit.jupiter.api.*;

@DisplayName("Tests for AuthErrorMapper")
public class AuthErrorMapperTest {

  private AuthErrorMapper mapper;

  @BeforeEach
  public void setup() {
    mapper = new AuthErrorMapper();
  }

  @Nested
  @DisplayName("mapInvalidCredentialsError")
  class MapInvalidCredentialsError {

    @Test
    @DisplayName("then return 401 with correct detail")
    public void return401WithDetail() {
      ErrorResult result = mapper.mapInvalidCredentialsError();

      assertThat(result.status()).isEqualTo(401);
      assertThat(result.response())
          .isEqualTo(new ErrorResponse(CommonErrorCode.UNAUTHORIZED).detail("Invalid credentials"));
    }
  }

  @Nested
  @DisplayName("mapJwtValidationError")
  class MapJwtValidationError {

    @Test
    @DisplayName("given Invalid, then does not throw any exception")
    public void givenInvalid_doesNotThrow() {
      assertThatCode(
              () -> mapper.mapJwtValidationError(new JwtService.JwtValidationError.Invalid()))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("given Missing, then does not throw any exception")
    public void givenMissing_doesNotThrow() {
      assertThatCode(
              () -> mapper.mapJwtValidationError(new JwtService.JwtValidationError.Missing()))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("given Expired, then does not throw any exception")
    public void givenExpired_doesNotThrow() {
      assertThatCode(
              () -> mapper.mapJwtValidationError(new JwtService.JwtValidationError.Expired()))
          .doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("mapAuthenticateUserError")
  class MapAuthenticateUserError {

    @Test
    @DisplayName("given InvalidCredentials, then delegate to mapInvalidCredentialsError")
    public void givenInvalidCredentials_delegatesToMapInvalidCredentialsError() {
      ErrorResult result =
          mapper.mapAuthenticateUserError(
              new AuthService.AuthenticateUserError.InvalidCredentials(
                  new InvalidCredentialsError()));

      assertThat(result.status()).isEqualTo(401);
      assertThat(result.response())
          .isEqualTo(new ErrorResponse(CommonErrorCode.UNAUTHORIZED).detail("Invalid credentials"));
    }

    @Test
    @DisplayName("given MaintenanceWindow, then return 503 with correct detail")
    public void givenMaintenanceWindow_return503WithDetail() {
      var availableFrom = java.time.Instant.parse("2025-06-02T08:00:00Z");
      ErrorResult result =
          mapper.mapAuthenticateUserError(
              new AuthService.AuthenticateUserError.MaintenanceWindow(availableFrom));

      assertThat(result.status()).isEqualTo(503);
      assertThat(result.response())
          .isEqualTo(
              new ErrorResponse(CommonErrorCode.UNAVAILABLE_SERVICE)
                  .detail("Service unavailable until " + availableFrom));
    }

    @Test
    @DisplayName("given ValidationFailed, then return 400 validation error response")
    public void givenValidationFailed_return400ValidationError() {
      ValidationNotification<UserDomainError> notification = new ValidationNotification<>();
      ErrorResult result =
          mapper.mapAuthenticateUserError(
              new AuthService.AuthenticateUserError.ValidationFailed(notification));

      assertThat(result.status()).isEqualTo(400);
    }
  }

  @Nested
  @DisplayName("mapRefreshTokensError")
  class MapRefreshTokensError {

    @Test
    @DisplayName("given MaintenanceWindow, then return 503 with correct detail")
    public void givenMaintenanceWindow_return503WithDetail() {
      var availableFrom = java.time.Instant.parse("2025-06-02T08:00:00Z");
      ErrorResult result =
          mapper.mapRefreshTokensError(
              new AuthService.RefreshTokensError.MaintenanceWindow(availableFrom));

      assertThat(result.status()).isEqualTo(503);
      assertThat(result.response())
          .isEqualTo(
              new ErrorResponse(CommonErrorCode.UNAVAILABLE_SERVICE)
                  .detail("Service unavailable until " + availableFrom));
    }

    @Test
    @DisplayName("given InvalidToken, then delegate to mapInvalidRefreshTokenError")
    public void givenInvalidToken_delegatesToMapInvalidRefreshTokenError() {
      InvalidRefreshTokenError tokenError = InvalidRefreshTokenError.notFound();
      ErrorResult result =
          mapper.mapRefreshTokensError(new AuthService.RefreshTokensError.InvalidToken(tokenError));

      assertThat(result.status()).isEqualTo(401);
      assertThat(result.response())
          .isEqualTo(
              new ErrorResponse(CommonErrorCode.UNAUTHORIZED).detail("Refresh token not found"));
    }
  }

  @Nested
  @DisplayName("mapInvalidRefreshTokenError")
  class MapInvalidRefreshTokenError {

    @Test
    @DisplayName("given NotFound reason, then return 401 with correct detail")
    public void givenNotFound_return401WithDetail() {
      ErrorResult result = mapper.mapInvalidRefreshTokenError(InvalidRefreshTokenError.notFound());

      assertThat(result.status()).isEqualTo(401);
      assertThat(result.response())
          .isEqualTo(
              new ErrorResponse(CommonErrorCode.UNAUTHORIZED).detail("Refresh token not found"));
    }

    @Test
    @DisplayName("given Expired reason, then return 401 with correct detail")
    public void givenExpired_return401WithDetail() {
      ErrorResult result = mapper.mapInvalidRefreshTokenError(InvalidRefreshTokenError.expired());

      assertThat(result.status()).isEqualTo(401);
      assertThat(result.response())
          .isEqualTo(
              new ErrorResponse(CommonErrorCode.UNAUTHORIZED).detail("Refresh token expired"));
    }

    @Test
    @DisplayName("given Revoked reason, then return 401 with correct detail")
    public void givenRevoked_return401WithDetail() {
      ErrorResult result = mapper.mapInvalidRefreshTokenError(InvalidRefreshTokenError.revoked());

      assertThat(result.status()).isEqualTo(401);
      assertThat(result.response())
          .isEqualTo(
              new ErrorResponse(CommonErrorCode.UNAUTHORIZED).detail("Refresh token revoked"));
    }
  }

  @Nested
  @DisplayName("supports")
  class Supports {

    @Test
    @DisplayName("given an AuthDomainError, then return true")
    public void givenAuthDomainError_returnTrue() {
      assertThat(mapper.supports(new InvalidCredentialsError())).isTrue();
    }

    @Test
    @DisplayName("given a JwtValidationError, then return true")
    public void givenJwtValidationError_returnTrue() {
      assertThat(mapper.supports(new JwtService.JwtValidationError.Invalid())).isTrue();
    }

    @Test
    @DisplayName("given an AuthenticateUserError, then return true")
    public void givenAuthenticateUserError_returnTrue() {
      assertThat(
              mapper.supports(
                  new AuthService.AuthenticateUserError.InvalidCredentials(
                      new InvalidCredentialsError())))
          .isTrue();
    }

    @Test
    @DisplayName("given a RefreshTokensError, then return true")
    public void givenRefreshTokensError_returnTrue() {
      assertThat(
              mapper.supports(
                  new AuthService.RefreshTokensError.InvalidToken(
                      InvalidRefreshTokenError.notFound())))
          .isTrue();
    }

    @Test
    @DisplayName("given an unrelated error, then return false")
    public void givenUnrelatedError_returnFalse() {
      assertThat(mapper.supports(new RuntimeException())).isFalse();
    }
  }

  @Nested
  @DisplayName("map")
  class Map {

    @Test
    @DisplayName("given an AuthenticateUserError, then delegate to mapAuthenticateUserError")
    public void givenAuthenticateUserError_delegatesToMapAuthenticateUserError() {
      ErrorResult result =
          mapper.map(
              new AuthService.AuthenticateUserError.InvalidCredentials(
                  new InvalidCredentialsError()));
      assertThat(result.status()).isEqualTo(401);
    }

    @Test
    @DisplayName("given a JwtValidationError, then does not throw any exception")
    public void givenJwtValidationError_doesNotThrow() {
      assertThatCode(() -> mapper.map(new JwtService.JwtValidationError.Invalid()))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("given a RefreshTokensError, then delegate to mapRefreshTokensError")
    public void givenRefreshTokensError_delegatesToMapRefreshTokensError() {
      ErrorResult result =
          mapper.map(
              new AuthService.RefreshTokensError.InvalidToken(InvalidRefreshTokenError.expired()));
      assertThat(result.status()).isEqualTo(401);
    }

    @Test
    @DisplayName("given an InvalidCredentialsError, then delegate to mapInvalidCredentialsError")
    public void givenInvalidCredentialsError_delegatesToMapInvalidCredentialsError() {
      ErrorResult result = mapper.map(new InvalidCredentialsError());
      assertThat(result.status()).isEqualTo(401);
    }

    @Test
    @DisplayName("given an unhandled error type, then throw UnhandledErrorException")
    public void givenUnhandledError_throwUnhandledErrorException() {
      assertThatThrownBy(() -> mapper.map(new RuntimeException()))
          .isInstanceOf(UnhandledErrorException.class);
    }
  }

  @Nested
  @DisplayName("supportsFieldError")
  class SupportsFieldError {

    @Test
    @DisplayName("given an AuthDomainError, then return true")
    public void givenAuthDomainError_returnTrue() {
      assertThat(mapper.supportsFieldError(InvalidRefreshTokenError.notFound())).isTrue();
    }

    @Test
    @DisplayName("given an unrelated DomainError, then return false")
    public void givenUnrelatedDomainError_returnFalse() {
      assertThat(mapper.supportsFieldError(new DomainError() {})).isFalse();
    }
  }

  @Nested
  @DisplayName("mapFieldError")
  class MapFieldError {

    @Test
    @DisplayName(
        "given InvalidValueError, then propagates the expected UnreachableCodeException from mapInvalidValue")
    public void givenInvalidValueError_propagatesUnreachableCodeException() {
      AuthDomainError.InvalidValueError error = InvalidRefreshTokenError.notFound();

      assertThatThrownBy(() -> mapper.mapFieldError(error))
          .isInstanceOf(UnreachableCodeException.class);
    }

    @Test
    @DisplayName("given InvalidCredentialsError, then throw UnreachableCodeException")
    public void givenInvalidCredentialsError_throwUnreachableCodeException() {
      assertThatThrownBy(() -> mapper.mapFieldError(new InvalidCredentialsError()))
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
  @DisplayName("mapInvalidValue")
  class MapInvalidValue {

    @Test
    @DisplayName("given InvalidRefreshTokenError NotFound, then throw UnreachableCodeException")
    public void givenInvalidRefreshTokenNotFound_throwUnreachableCodeException() {
      assertThatThrownBy(() -> mapper.mapInvalidValue(InvalidRefreshTokenError.notFound()))
          .isInstanceOf(UnreachableCodeException.class);
    }

    @Test
    @DisplayName("given InvalidRefreshTokenError Expired, then throw UnreachableCodeException")
    public void givenInvalidRefreshTokenExpired_throwUnreachableCodeException() {
      assertThatThrownBy(() -> mapper.mapInvalidValue(InvalidRefreshTokenError.expired()))
          .isInstanceOf(UnreachableCodeException.class);
    }

    @Test
    @DisplayName("given InvalidRefreshTokenError Revoked, then throw UnreachableCodeException")
    public void givenInvalidRefreshTokenRevoked_throwUnreachableCodeException() {
      assertThatThrownBy(() -> mapper.mapInvalidValue(InvalidRefreshTokenError.revoked()))
          .isInstanceOf(UnreachableCodeException.class);
    }

    @Test
    @DisplayName("given an unhandled InvalidValueError, then throw UnhandledErrorException")
    public void givenUnhandledInvalidValueError_throwUnhandledErrorException() {
      assertThatThrownBy(() -> mapper.mapInvalidValue(new InvalidValueError() {}))
          .isInstanceOf(UnhandledErrorException.class);
    }
  }
}
