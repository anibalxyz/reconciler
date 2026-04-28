package com.anibalxyz.features.auth.domain.error;

import com.anibalxyz.features.common.domain.error.DomainError;

public sealed interface AuthDomainError extends DomainError
    permits AuthDomainError.InvalidValueError, InvalidCredentialsError {
  sealed interface InvalidValueError
      extends com.anibalxyz.features.common.domain.error.InvalidValueError, AuthDomainError
      permits InvalidRefreshTokenError {}
}
