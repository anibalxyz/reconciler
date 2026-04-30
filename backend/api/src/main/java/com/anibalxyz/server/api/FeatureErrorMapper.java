package com.anibalxyz.server.api;

import com.anibalxyz.features.common.api.out.response.error.ErrorDetail;
import com.anibalxyz.features.common.domain.error.DomainError;

public interface FeatureErrorMapper {
  boolean supports(Object error);

  ErrorResult map(Object error);

  boolean supportsFieldError(DomainError error);

  ErrorDetail mapFieldError(DomainError error);
}
