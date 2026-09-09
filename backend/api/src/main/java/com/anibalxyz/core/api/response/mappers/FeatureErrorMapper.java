package com.anibalxyz.core.api.response.mappers;

import com.anibalxyz.core.api.response.error.ErrorDetail;
import com.anibalxyz.core.domain.DomainError;

public interface FeatureErrorMapper {
  boolean supports(Object error);

  ErrorMapperResult map(Object error);

  boolean supportsFieldError(DomainError error);

  ErrorDetail mapFieldError(DomainError error);
}
