package com.anibalxyz.features.users.domain.error;

import com.anibalxyz.features.common.domain.error.ReasonedError;

/**
 * Marker interface for all domain errors belonging to the Users feature.
 *
 * <p>This allows the global error dispatcher to route generic {@link ReasonedError}s
 * to the {@link com.anibalxyz.features.users.api.UserErrorMapper} without knowing
 * the specific error types.
 */
public sealed interface UserDomainError permits InvalidEmailError, InvalidPasswordError, UserNotFoundError {
}
