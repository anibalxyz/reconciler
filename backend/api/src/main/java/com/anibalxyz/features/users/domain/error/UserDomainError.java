package com.anibalxyz.features.users.domain.error;

import com.anibalxyz.core.domain.error.DomainError;

public sealed interface UserDomainError extends DomainError
    permits EmailAlreadyTakenError, UserDomainError.InvalidValueError, UserNotFoundError {
  sealed interface InvalidValueError
      extends com.anibalxyz.core.domain.error.InvalidValueError, UserDomainError
      permits InvalidEmailError, InvalidNameError, InvalidPasswordError, InvalidPasswordHashError {}
}
