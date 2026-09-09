package com.anibalxyz.server.http.mappers;

import com.anibalxyz.core.primitives.ReconcilerException;
import com.anibalxyz.core.api.response.mappers.ErrorMapperResult;
import com.anibalxyz.core.api.response.mappers.FeatureErrorMapper;
import com.anibalxyz.core.api.response.mappers.ValidationErrorMapper;
import com.anibalxyz.core.api.response.error.ErrorDetail;
import com.anibalxyz.core.application.ValidationNotification;
import com.anibalxyz.core.domain.DomainError;
import com.anibalxyz.features.auth.api.AuthErrorMapper;
import com.anibalxyz.features.users.api.UserErrorMapper;
import java.util.List;

public class FeaturesErrorMapper {
  private static final List<FeatureErrorMapper> mappers =
      List.of(new UserErrorMapper(), new AuthErrorMapper());

  private FeaturesErrorMapper() {}

  public static ErrorMapperResult map(Object error) {
    if (error instanceof ValidationNotification<?> n) {
      return ValidationErrorMapper.map(n, FeaturesErrorMapper::mapFieldError);
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

  public static class UnregisteredMapperException extends ReconcilerException {
    public UnregisteredMapperException(Object error) {
      super("No mapper registered for error type: " + error.getClass().getName());
    }
  }
}
