package com.anibalxyz.features.common.api;

import com.anibalxyz.features.common.api.out.ErrorDetail;
import com.anibalxyz.features.common.api.out.ErrorResponse;
import com.anibalxyz.features.common.api.out.ValidationErrorCode;
import com.anibalxyz.features.common.application.ValidationNotification;
import com.anibalxyz.features.common.domain.error.ReasonedError;
import com.anibalxyz.server.api.ErrorResult;
import java.util.List;
import java.util.function.Function;

public class ValidationErrorMapper {

  public static ErrorResult map(
      ValidationNotification notification,
      Function<ReasonedError<?>, ErrorDetail> invalidValueMapper) {
    List<ErrorDetail> details =
        notification.getErrors().stream()
            .map(entry -> mapEntry(entry, invalidValueMapper))
            .toList();
    return new ErrorResult(
        400,
        new ErrorResponse(ValidationErrorCode.VALIDATION_ERROR)
            .type("/api/errors/validation-error")
            .errors(details));
  }

  private static ErrorDetail mapEntry(
      ValidationNotification.ErrorEntry entry,
      Function<ReasonedError<?>, ErrorDetail> invalidValueMapper) {
    String field = entry.field(); // TODO: implement JSON pointer
    return switch (entry.failure()) {
      case ValidationNotification.FieldFailure.Missing ignored ->
          new ErrorDetail(ValidationErrorCode.REQUIRED_FIELD)
              .with("title", ValidationErrorCode.REQUIRED_FIELD.title())
              .with("field", field);
      case ValidationNotification.FieldFailure.Blank ignored ->
          new ErrorDetail(ValidationErrorCode.BLANK_FIELD)
              .with("title", ValidationErrorCode.BLANK_FIELD.title())
              .with("field", field);
      case ValidationNotification.FieldFailure.Conflict ignored ->
          new ErrorDetail(ValidationErrorCode.CONFLICT_FIELD)
              .with("title", ValidationErrorCode.CONFLICT_FIELD.title())
              .with("field", field);
      case ValidationNotification.FieldFailure.InvalidValue v ->
          invalidValueMapper.apply(v.error()).with("field", field);
    };
  }
}
