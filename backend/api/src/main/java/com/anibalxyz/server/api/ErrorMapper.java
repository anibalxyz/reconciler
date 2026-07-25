package com.anibalxyz.server.api;

import com.anibalxyz.core.application.ValidationNotification;
import com.anibalxyz.core.domain.error.DomainError;
import com.anibalxyz.features.auth.api.AuthErrorMapper;
import com.anibalxyz.features.common.api.ValidationErrorMapper;
import com.anibalxyz.features.common.api.out.response.error.ErrorDetail;
import com.anibalxyz.features.users.api.UserErrorMapper;
import com.anibalxyz.server.exception.UnregisteredMapperException;
import java.util.List;

public class ErrorMapper {
  private static final List<FeatureErrorMapper> mappers =
      List.of(new UserErrorMapper(), new AuthErrorMapper());

  private ErrorMapper() {}

  public static ErrorResult map(Object error) {
    if (error instanceof ValidationNotification<?> n) {
      return ValidationErrorMapper.map(n, ErrorMapper::mapFieldError);
    }
    return mappers.stream()
        .filter(m -> m.supports(error))
        .findFirst()
        .orElseThrow(() -> new UnregisteredMapperException(error))
        .map(error);
  }

  public static <E extends DomainError> ErrorDetail mapFieldError(E error) {
    return mappers.stream()
        .filter(m -> m.supportsFieldError(error))
        .findFirst()
        .orElseThrow(() -> new UnregisteredMapperException(error))
        .mapFieldError(error);
  }
}
