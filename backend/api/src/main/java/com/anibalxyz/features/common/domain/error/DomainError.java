package com.anibalxyz.features.common.domain.error;

import com.anibalxyz.features.common.Result;

/**
 * Marker interface for all domain-specific errors.
 *
 * <p>Domain errors represent business rule violations or invalid states detected within the domain
 * layer. They are treated as pure data, not exceptions; <b>never throw them</b>. Use {@link Result}
 * or similar approach to propagate them.
 */
public interface DomainError {}
