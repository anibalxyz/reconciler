package com.anibalxyz.core.api.response.mappers;

import com.anibalxyz.core.api.response.error.ErrorDetail;
import com.anibalxyz.core.api.response.error.ErrorResponse;
import com.anibalxyz.core.api.response.error.ValidationErrorCode;
import com.anibalxyz.core.application.ValidationNotification;
import com.anibalxyz.core.domain.DomainError;
import java.util.List;
import java.util.function.Function;

public class ValidationErrorMapper {
  private ValidationErrorMapper() {}

  public static <E extends DomainError> ErrorMapperResult map(
      ValidationNotification<E> notification, Function<E, ErrorDetail> fieldErrorMapper) {
    // TODO: implement JSON pointer
    List<ErrorDetail> details =
        notification.getErrors().stream()
            .map(entry -> fieldErrorMapper.apply(entry.error()).with("field", entry.field()))
            .toList();
    return new ErrorMapperResult(
        400,
        new ErrorResponse(ValidationErrorCode.VALIDATION_ERROR)
            .type("/api/errors/validation-error")
            .errors(details));
  }
}
