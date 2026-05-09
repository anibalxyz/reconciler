package com.anibalxyz.features.auth.api;

import static net.logstash.logback.argument.StructuredArguments.kv;

import com.anibalxyz.features.auth.api.out.AuthErrorCode;
import com.anibalxyz.features.auth.application.AuthService;
import com.anibalxyz.features.auth.application.JwtService;
import com.anibalxyz.features.auth.domain.error.AuthDomainError;
import com.anibalxyz.features.auth.domain.error.InvalidCredentialsError;
import com.anibalxyz.features.auth.domain.error.InvalidRefreshTokenError;
import com.anibalxyz.features.common.api.ValidationErrorMapper;
import com.anibalxyz.features.common.api.out.code.CommonErrorCode;
import com.anibalxyz.features.common.api.out.response.error.ErrorDetail;
import com.anibalxyz.features.common.api.out.response.error.ErrorResponse;
import com.anibalxyz.features.common.domain.error.DomainError;
import com.anibalxyz.features.common.domain.error.InvalidValueError;
import com.anibalxyz.server.api.ErrorMapper;
import com.anibalxyz.server.api.ErrorResult;
import com.anibalxyz.server.api.FeatureErrorMapper;
import com.anibalxyz.server.exception.UnhandledErrorException;
import com.anibalxyz.server.exception.UnreachableCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthErrorMapper implements FeatureErrorMapper {

  private static final Logger log = LoggerFactory.getLogger(AuthErrorMapper.class);

  public ErrorResult mapInvalidCredentialsError() {
    log.warn("Invalid credentials attempt");
    return new ErrorResult(
        401, new ErrorResponse(CommonErrorCode.UNAUTHORIZED).detail("Invalid credentials"));
  }

  public ErrorResult mapAuthenticateUserError(AuthService.AuthenticateUserError error) {
    return switch (error) {
      case AuthService.AuthenticateUserError.InvalidCredentials ignored ->
          mapInvalidCredentialsError();
      case AuthService.AuthenticateUserError.MaintenanceWindow e -> {
        log.debug(
            "Authentication during maintenance window", kv("available_from", e.availableFrom()));
        yield new ErrorResult(
            503,
            new ErrorResponse(CommonErrorCode.UNAVAILABLE_SERVICE)
                .detail("Service unavailable until " + e.availableFrom()));
      }
      case AuthService.AuthenticateUserError.ValidationFailed e ->
          ValidationErrorMapper.map(e.notification(), ErrorMapper::mapFieldError);
    };
  }

  public ErrorResult mapRefreshTokensError(AuthService.RefreshTokensError error) {
    return switch (error) {
      case AuthService.RefreshTokensError.MaintenanceWindow e -> {
        log.debug(
            "Token refresh during maintenance window", kv("available_from", e.availableFrom()));
        yield new ErrorResult(
            503,
            new ErrorResponse(CommonErrorCode.UNAVAILABLE_SERVICE)
                .detail("Service unavailable until " + e.availableFrom()));
      }
      case AuthService.RefreshTokensError.InvalidToken e -> mapInvalidRefreshTokenError(e.error());
    };
  }

  public ErrorResult mapInvalidRefreshTokenError(InvalidRefreshTokenError error) {
    return new ErrorResult(
        401,
        switch (error.getReason()) {
          case InvalidRefreshTokenError.Reason.NotFound ignored -> {
            log.debug("Refresh token not found");
            yield new ErrorResponse(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND);
          }
          case InvalidRefreshTokenError.Reason.Expired ignored -> {
            log.debug("Refresh token expired");
            yield new ErrorResponse(AuthErrorCode.REFRESH_TOKEN_EXPIRED);
          }
          case InvalidRefreshTokenError.Reason.Revoked ignored -> {
            log.warn("Revoked refresh token used");
            yield new ErrorResponse(AuthErrorCode.REFRESH_TOKEN_EXPIRED);
          }
        });
  }

  @Override
  public boolean supports(Object error) {
    return error instanceof AuthDomainError
        || error instanceof AuthService.AuthenticateUserError
        || error instanceof AuthService.RefreshTokensError
        || error instanceof JwtService.JwtValidationError;
  }

  @Override
  public ErrorResult map(Object error) {
    return switch (error) {
      case AuthService.AuthenticateUserError e -> mapAuthenticateUserError(e);
      case AuthService.RefreshTokensError e -> mapRefreshTokensError(e);
      case JwtService.JwtValidationError e -> mapJwtValidationError(e);
      case InvalidCredentialsError ignored -> mapInvalidCredentialsError();
      default -> throw new UnhandledErrorException(error);
    };
  }

  @Override
  public boolean supportsFieldError(DomainError error) {
    return error instanceof AuthDomainError;
  }

  @Override
  public ErrorDetail mapFieldError(DomainError error) {
    if (error instanceof AuthDomainError ade) {
      return switch (ade) {
        // Branch is covered, but JaCoCo reports a false negative due to
        // bytecode instrumentation issues with pattern matching switches.
        case AuthDomainError.InvalidValueError ive -> mapInvalidValue(ive);
        case InvalidCredentialsError e ->
            throw UnreachableCodeException.of(e, "credentials errors are not field errors");
      };
    }
    throw new UnhandledErrorException(error);
  }

  public ErrorResult mapJwtValidationError(JwtService.JwtValidationError jwe) {
    ErrorResponse base = new ErrorResponse(CommonErrorCode.UNAUTHORIZED);
    base =
        switch (jwe) {
          case JwtService.JwtValidationError.Invalid ignored -> {
            log.warn("Invalid JWT token");
            yield base.detail("Invalid JWT token");
          }
          case JwtService.JwtValidationError.Missing ignored -> {
            log.warn("Missing JWT token");
            yield base.detail("Missing JWT token");
          }
          case JwtService.JwtValidationError.Expired ignored -> {
            log.debug("JWT has expired");
            yield base.detail("JWT has expired");
          }
        };

    return new ErrorResult(401, base);
  }

  public ErrorDetail mapInvalidValue(InvalidValueError error) {
    // No switch here — InvalidRefreshTokenError is the only permitted type, and it is
    // unreachable as a field error. If new InvalidValueError subtypes are added to
    // AuthDomainError, this should be refactored into a switch.
    if (error instanceof InvalidRefreshTokenError e) {
      throw UnreachableCodeException.of(
          e, "refresh token errors are not field-level validation errors");
    }
    throw new UnhandledErrorException(error);
  }
}
