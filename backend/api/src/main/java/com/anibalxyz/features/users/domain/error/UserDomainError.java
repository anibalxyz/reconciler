package com.anibalxyz.features.users.domain.error;

import com.anibalxyz.features.common.domain.error.DomainError;

public sealed interface UserDomainError extends DomainError
    permits EmailAlreadyTakenError, UserDomainError.InvalidValueError, UserNotFoundError {
  sealed interface InvalidValueError
      extends com.anibalxyz.features.common.domain.error.InvalidValueError, UserDomainError
      permits InvalidEmailError, InvalidNameError, InvalidPasswordError, InvalidPasswordHashError {}
}
