package com.anibalxyz.features.common.api;

import com.anibalxyz.core.application.ValidationNotification;
import com.anibalxyz.core.domain.error.DomainError;
import com.anibalxyz.features.common.api.out.code.ValidationErrorCode;
import com.anibalxyz.features.common.api.out.response.error.ErrorDetail;
import com.anibalxyz.features.common.api.out.response.error.ErrorResponse;
import com.anibalxyz.server.api.ErrorResult;
import java.util.List;
import java.util.function.Function;

public class ValidationErrorMapper {
  private ValidationErrorMapper() {}

  public static <E extends DomainError> ErrorResult map(
      ValidationNotification<E> notification, Function<E, ErrorDetail> fieldErrorMapper) {
    // TODO: implement JSON pointer
    List<ErrorDetail> details =
        notification.getErrors().stream()
            .map(entry -> fieldErrorMapper.apply(entry.error()).with("field", entry.field()))
            .toList();
    return new ErrorResult(
        400,
        new ErrorResponse(ValidationErrorCode.VALIDATION_ERROR)
            .type("/api/errors/validation-error")
            .errors(details));
  }
}
