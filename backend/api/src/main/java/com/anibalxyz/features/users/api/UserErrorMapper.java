package com.anibalxyz.features.users.api;

import com.anibalxyz.features.common.api.ValidationErrorMapper;
import com.anibalxyz.features.common.api.out.CommonErrorCode;
import com.anibalxyz.features.common.api.out.ErrorDetail;
import com.anibalxyz.features.common.api.out.ErrorResponse;
import com.anibalxyz.features.common.api.out.ValidationErrorCode;
import com.anibalxyz.features.common.application.ValidationNotification;
import com.anibalxyz.features.users.application.UserService;
import com.anibalxyz.features.users.domain.error.InvalidEmailError;
import com.anibalxyz.features.users.domain.error.InvalidPasswordError;
import com.anibalxyz.features.users.domain.error.UserDomainError;
import com.anibalxyz.features.users.domain.error.UserNotFoundError;
import com.anibalxyz.server.api.ErrorResult;

public class UserErrorMapper {

  public static ErrorResult map(UserNotFoundError error) {
    ErrorResponse base = new ErrorResponse(CommonErrorCode.RESOURCE_NOT_FOUND);
    return new ErrorResult(
        404,
        switch (error.getReason()) {
          case UserNotFoundError.Reason.ById r ->
              base.detail("User with id " + r.id() + " not found");
          case UserNotFoundError.Reason.ByEmail r ->
              base.detail("User with email " + r.email() + " not found");
        });
  }

  public static ErrorResult map(UserService.UpdateUserByIdError error) {
    return switch (error) {
      case UserService.UpdateUserByIdError.EmptyCommand ignored ->
          new ErrorResult(
              400,
              new ErrorResponse(ValidationErrorCode.VALIDATION_ERROR)
                  .detail("At least one field (name, email, password) must be provided"));
      case UserService.UpdateUserByIdError.NotFound e -> map(e.error());
      case UserService.UpdateUserByIdError.ValidationFailed e -> map(e.notification());
    };
  }

  public static ErrorResult map(ValidationNotification notification) {
    return ValidationErrorMapper.map(
        notification,
        error -> {
          if (error instanceof UserDomainError e) {
            // TODO: review this.
            // Note that it is assuming that all ReasonedErrors are invalid value errors
            return mapInvalidValue(e);
          }
          throw new IllegalStateException(
              "UserErrorMapper received non-user error: " + error.getClass().getName());
        });
  }

  public static ErrorDetail mapInvalidValue(UserDomainError error) {
    return switch (error) {
      case InvalidEmailError e ->
          switch (e.getReason()) {
            case InvalidEmailError.Reason.InvalidFormat ignored ->
                new ErrorDetail(ValidationErrorCode.INVALID_EMAIL_FORMAT)
                    .with("title", ValidationErrorCode.INVALID_EMAIL_FORMAT.title());
          };
      case InvalidPasswordError e ->
          switch (e.getReason()) {
            case InvalidPasswordError.Reason.Empty ignored ->
                new ErrorDetail(ValidationErrorCode.INVALID_PASSWORD_EMPTY)
                    .with("title", ValidationErrorCode.INVALID_PASSWORD_EMPTY.title());
            case InvalidPasswordError.Reason.TooShort r ->
                new ErrorDetail(ValidationErrorCode.INVALID_PASSWORD_TOO_SHORT)
                    .with("title", ValidationErrorCode.INVALID_PASSWORD_TOO_SHORT.title())
                    .with("detail", "Must be at least " + r.minLength() + " characters")
                    .with("minLength", r.minLength());
            case InvalidPasswordError.Reason.TooLong r ->
                new ErrorDetail(ValidationErrorCode.INVALID_PASSWORD_TOO_LONG)
                    .with("title", ValidationErrorCode.INVALID_PASSWORD_TOO_LONG.title())
                    .with("detail", "Cannot exceed " + r.maxLength() + " characters")
                    .with("maxLength", r.maxLength());
          };
      case UserNotFoundError e ->
          new ErrorDetail(CommonErrorCode.RESOURCE_NOT_FOUND)
              .with("title", CommonErrorCode.RESOURCE_NOT_FOUND.title())
              .with("detail", "User not found");
    };
  }
}
