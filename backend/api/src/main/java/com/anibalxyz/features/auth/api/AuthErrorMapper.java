package com.anibalxyz.features.auth.api;

import static net.logstash.logback.argument.StructuredArguments.kv;

import com.anibalxyz.core.domain.error.DomainError;
import com.anibalxyz.core.domain.error.InvalidValueError;
import com.anibalxyz.features.auth.api.out.AuthErrorCode;
import com.anibalxyz.features.auth.application.AuthenticateUser;
import com.anibalxyz.features.auth.application.JwtService;
import com.anibalxyz.features.auth.application.RefreshTokens;
import com.anibalxyz.features.auth.domain.error.AuthDomainError;
import com.anibalxyz.features.auth.domain.error.InvalidCredentialsError;
import com.anibalxyz.features.auth.domain.error.InvalidRefreshTokenError;
import com.anibalxyz.features.common.api.ValidationErrorMapper;
import com.anibalxyz.features.common.api.out.code.CommonErrorCode;
import com.anibalxyz.features.common.api.out.response.error.ErrorDetail;
import com.anibalxyz.features.common.api.out.response.error.ErrorResponse;
import com.anibalxyz.server.api.ErrorMapper;
import com.anibalxyz.server.api.ErrorResult;
import com.anibalxyz.server.api.FeatureErrorMapper;
import com.anibalxyz.server.api.LogEntry;
import com.anibalxyz.server.exception.UnhandledErrorException;
import com.anibalxyz.server.exception.UnreachableCodeException;

public class AuthErrorMapper implements FeatureErrorMapper {

  public ErrorResult mapInvalidCredentialsError() {
    return new ErrorResult(
        401,
        new ErrorResponse(CommonErrorCode.UNAUTHENTICATED).detail("Invalid credentials"),
        LogEntry.warn("Invalid credentials attempt"));
  }

  public ErrorResult mapAuthenticateUserError(AuthenticateUser.Error error) {
    return switch (error) {
      case AuthenticateUser.Error.InvalidCredentials ignored -> mapInvalidCredentialsError();
      case AuthenticateUser.Error.MaintenanceWindow e ->
          new ErrorResult(
              503,
              new ErrorResponse(CommonErrorCode.UNAVAILABLE_SERVICE)
                  .detail("Service unavailable until " + e.availableFrom()),
              LogEntry.debug(
                  "Authentication during maintenance window",
                  kv("available_from", e.availableFrom())));
      case AuthenticateUser.Error.ValidationFailed e ->
          ValidationErrorMapper.map(e.notification(), ErrorMapper::mapFieldError);
    };
  }

  public ErrorResult mapRefreshTokensError(RefreshTokens.Error error) {
    return switch (error) {
      case RefreshTokens.Error.MaintenanceWindow e ->
          new ErrorResult(
              503,
              new ErrorResponse(CommonErrorCode.UNAVAILABLE_SERVICE)
                  .detail("Service unavailable until " + e.availableFrom()),
              LogEntry.debug(
                  "Token refresh during maintenance window",
                  kv("available_from", e.availableFrom())));
      case RefreshTokens.Error.InvalidToken e -> mapInvalidRefreshTokenError(e.error());
    };
  }

  public ErrorResult mapInvalidRefreshTokenError(InvalidRefreshTokenError error) {
    return switch (error.getReason()) {
      case InvalidRefreshTokenError.Reason.NotFound ignored ->
          new ErrorResult(
              401,
              new ErrorResponse(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND),
              LogEntry.debug("Refresh token not found"));
      case InvalidRefreshTokenError.Reason.Expired ignored ->
          new ErrorResult(
              401,
              new ErrorResponse(AuthErrorCode.REFRESH_TOKEN_EXPIRED),
              LogEntry.debug("Refresh token expired"));
      case InvalidRefreshTokenError.Reason.Revoked ignored ->
          new ErrorResult(
              401,
              new ErrorResponse(AuthErrorCode.REFRESH_TOKEN_EXPIRED),
              LogEntry.warn("Revoked refresh token used"));
      case InvalidRefreshTokenError.Reason.Invalid ignored ->
          new ErrorResult(
              401,
              new ErrorResponse(AuthErrorCode.REFRESH_TOKEN_INVALID),
              LogEntry.warn("Invalid refresh token was used"));
    };
  }

  @Override
  public boolean supports(Object error) {
    return error instanceof AuthDomainError
        || error instanceof AuthenticateUser.Error
        || error instanceof RefreshTokens.Error
        || error instanceof JwtService.JwtValidationError;
  }

  @Override
  public ErrorResult map(Object error) {
    return switch (error) {
      case AuthenticateUser.Error e -> mapAuthenticateUserError(e);
      case RefreshTokens.Error e -> mapRefreshTokensError(e);
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
    ErrorResponse base = new ErrorResponse(CommonErrorCode.UNAUTHENTICATED);
    return switch (jwe) {
      case JwtService.JwtValidationError.Invalid ignored ->
          new ErrorResult(
              401, base.detail("Invalid JWT token"), LogEntry.warn("Invalid JWT token"));
      case JwtService.JwtValidationError.Missing ignored ->
          new ErrorResult(
              401, base.detail("Missing JWT token"), LogEntry.warn("Missing JWT token"));
      case JwtService.JwtValidationError.Expired ignored ->
          new ErrorResult(401, base.detail("JWT has expired"), LogEntry.debug("JWT has expired"));
    };
  }

  public ErrorDetail mapInvalidValue(InvalidValueError error) {
    if (error instanceof InvalidRefreshTokenError e) {
      throw UnreachableCodeException.of(
          e, "refresh token errors are not field-level validation errors");
    }
    throw new UnhandledErrorException(error);
  }
}
