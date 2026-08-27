package com.anibalxyz.features.users.api;

import static net.logstash.logback.argument.StructuredArguments.kv;

import com.anibalxyz.core.domain.error.DomainError;
import com.anibalxyz.core.domain.error.InvalidValueError;
import com.anibalxyz.features.common.api.ValidationErrorMapper;
import com.anibalxyz.features.common.api.out.code.CommonErrorCode;
import com.anibalxyz.features.common.api.out.code.ValidationErrorCode;
import com.anibalxyz.features.common.api.out.response.error.ErrorDetail;
import com.anibalxyz.features.common.api.out.response.error.ErrorResponse;
import com.anibalxyz.features.users.application.UpdateUserById;
import com.anibalxyz.features.users.domain.error.*;
import com.anibalxyz.server.api.ErrorResult;
import com.anibalxyz.server.api.FeatureErrorMapper;
import com.anibalxyz.server.api.LogEntry;
import com.anibalxyz.server.exception.UnhandledErrorException;
import com.anibalxyz.server.exception.UnreachableCodeException;

public class UserErrorMapper implements FeatureErrorMapper {

  @Override
  public boolean supports(Object error) {
    return error instanceof UserDomainError || error instanceof UpdateUserById.Error;
  }

  @Override
  public ErrorResult map(Object error) {
    return switch (error) {
      case UpdateUserById.Error e -> mapUpdateUserByIdError(e);
      case UserNotFoundError e -> mapUserNotFoundError(e);
      default -> throw new UnhandledErrorException(error);
    };
  }

  @Override
  public boolean supportsFieldError(DomainError error) {
    return error instanceof UserDomainError;
  }

  @Override
  public ErrorDetail mapFieldError(DomainError error) {
    if (error instanceof UserDomainError ude) {
      return switch (ude) {
        case UserDomainError.InvalidValueError ive -> mapInvalidValue(ive);
        case EmailAlreadyTakenError ignored ->
            new ErrorDetail(ValidationErrorCode.CONFLICT_FIELD)
                .with("title", ValidationErrorCode.CONFLICT_FIELD.title());
        case UserNotFoundError e ->
            throw UnreachableCodeException.of(e, "not found errors are not field errors");
      };
    }
    throw new UnhandledErrorException(error);
  }

  public ErrorResult mapUpdateUserByIdError(UpdateUserById.Error error) {
    return switch (error) {
      case UpdateUserById.Error.EmptyCommand ignored ->
          new ErrorResult(
              400,
              new ErrorResponse(ValidationErrorCode.VALIDATION_ERROR)
                  .detail("At least one field (name, email, password) must be provided"),
              LogEntry.debug("Update user with no fields provided"));
      case UpdateUserById.Error.NotFound e -> mapUserNotFoundError(e.error());
      case UpdateUserById.Error.ValidationFailed e ->
          ValidationErrorMapper.map(e.notification(), this::mapFieldError);
    };
  }

  public ErrorResult mapUserNotFoundError(UserNotFoundError error) {
    ErrorResponse base = new ErrorResponse(CommonErrorCode.RESOURCE_NOT_FOUND);
    return switch (error.getReason()) {
      case UserNotFoundError.Reason.ById r ->
          new ErrorResult(
              404,
              base.detail("User with id " + r.id() + " not found"),
              LogEntry.debug("User not found", kv("user_nf_id", r.id())));
      case UserNotFoundError.Reason.ByEmail r ->
          throw UnreachableCodeException.of(r, "no endpoint exposes email-based user lookups");
    };
  }

  public ErrorDetail mapInvalidValue(InvalidValueError error) {
    if (error instanceof UserDomainError.InvalidValueError ive) {
      return switch (ive) {
        case InvalidNameError e ->
            switch (e.getReason()) {
              case InvalidNameError.Reason.TooLong r ->
                  new ErrorDetail(ValidationErrorCode.TOO_LONG)
                      .with("title", ValidationErrorCode.TOO_LONG.title())
                      .with("detail", "Cannot exceed " + r.maxLength() + " characters")
                      .with("maxLength", r.maxLength());
              case InvalidNameError.Reason.Blank ignored ->
                  new ErrorDetail(ValidationErrorCode.BLANK_FIELD)
                      .with("title", ValidationErrorCode.BLANK_FIELD.title());
              case InvalidNameError.Reason.Absent ignored ->
                  new ErrorDetail(ValidationErrorCode.REQUIRED_FIELD)
                      .with("title", ValidationErrorCode.REQUIRED_FIELD.title());
            };
        case InvalidEmailError e ->
            switch (e.getReason()) {
              case InvalidEmailError.Reason.InvalidFormat ignored ->
                  new ErrorDetail(ValidationErrorCode.INVALID_FIELD_FORMAT)
                      .with("title", ValidationErrorCode.INVALID_FIELD_FORMAT.title());
              case InvalidEmailError.Reason.Blank ignored ->
                  new ErrorDetail(ValidationErrorCode.BLANK_FIELD)
                      .with("title", ValidationErrorCode.BLANK_FIELD.title());
              case InvalidEmailError.Reason.Absent ignored ->
                  new ErrorDetail(ValidationErrorCode.REQUIRED_FIELD)
                      .with("title", ValidationErrorCode.REQUIRED_FIELD.title());
              case InvalidEmailError.Reason.TooLong r ->
                  new ErrorDetail(ValidationErrorCode.TOO_LONG)
                      .with("title", ValidationErrorCode.TOO_LONG.title())
                      .with("detail", "Cannot exceed " + r.maxLength() + " characters")
                      .with("maxLength", r.maxLength());
            };
        case InvalidPasswordError e ->
            switch (e.getReason()) {
              case InvalidPasswordError.Reason.Blank ignored ->
                  new ErrorDetail(ValidationErrorCode.BLANK_FIELD)
                      .with("title", ValidationErrorCode.BLANK_FIELD.title());
              case InvalidPasswordError.Reason.TooShort r ->
                  new ErrorDetail(ValidationErrorCode.TOO_SHORT)
                      .with("title", ValidationErrorCode.TOO_SHORT.title())
                      .with("detail", "Must be at least " + r.minLength() + " characters")
                      .with("minLength", r.minLength());
              case InvalidPasswordError.Reason.TooLong r ->
                  new ErrorDetail(ValidationErrorCode.TOO_LONG)
                      .with("title", ValidationErrorCode.TOO_LONG.title())
                      .with("detail", "Cannot exceed " + r.maxLength() + " characters")
                      .with("maxLength", r.maxLength());
              case InvalidPasswordError.Reason.Absent ignored ->
                  new ErrorDetail(ValidationErrorCode.REQUIRED_FIELD)
                      .with("title", ValidationErrorCode.REQUIRED_FIELD.title());
            };
        case InvalidPasswordHashError e ->
            throw UnreachableCodeException.of(
                e, "corrupted hash should be caught at infra layer before reaching the mapper");
        case InvalidUserIdError invalidUserIdError ->
            throw UnreachableCodeException.of(
                invalidUserIdError, "user id errors are not field-level validation errors");
      };
    }
    throw new UnhandledErrorException(error);
  }
}
