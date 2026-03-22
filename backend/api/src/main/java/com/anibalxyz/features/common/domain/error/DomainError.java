package com.anibalxyz.features.common.domain.error;

import com.anibalxyz.features.common.Result;

/**
 * Base class for all domain errors.
 *
 * <p>Domain errors represent rule violations or invalid states detected within the domain layer.
 * They are not exceptions and should not be thrown — use {@link Result} to propagate them.
 */
public abstract class DomainError {}
