package com.anibalxyz.server.api;

import com.anibalxyz.core.domain.error.DomainError;
import com.anibalxyz.features.common.api.out.response.error.ErrorDetail;

public interface FeatureErrorMapper {
  boolean supports(Object error);

  ErrorResult map(Object error);

  boolean supportsFieldError(DomainError error);

  ErrorDetail mapFieldError(DomainError error);
}
