package com.anibalxyz.server.api;

import com.anibalxyz.features.common.api.ValidationErrorMapper;
import com.anibalxyz.features.common.api.out.ErrorDetail;
import com.anibalxyz.features.common.application.ValidationNotification;
import com.anibalxyz.features.common.domain.error.ReasonedError;
import com.anibalxyz.features.users.api.UserErrorMapper;
import com.anibalxyz.features.users.application.UserService;
import com.anibalxyz.features.users.domain.error.UserDomainError;
import com.anibalxyz.features.users.domain.error.UserNotFoundError;

public class ErrorMapper {

  public static ErrorResult map(Object error) {
    return switch (error) {
      case UserNotFoundError e -> UserErrorMapper.map(e);
      case UserService.UpdateUserByIdError e -> UserErrorMapper.map(e);
      case ValidationNotification n -> ValidationErrorMapper.map(n, ErrorMapper::mapDomainError);
      default -> throw new RuntimeException("Unhandled error type: " + error.getClass().getName());
    };
  }

  private static ErrorDetail mapDomainError(ReasonedError<?> error) {
    return switch (error) {
      case UserDomainError e -> UserErrorMapper.mapInvalidValue(e);
      default -> throw new IllegalStateException("Unhandled domain error feature: " + error.getClass().getName());
    };
  }
}
