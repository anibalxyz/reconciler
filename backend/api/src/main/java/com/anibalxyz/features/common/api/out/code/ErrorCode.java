package com.anibalxyz.features.common.api.out.code;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Marker interface for error code enums.
 *
 * <p>Enables polymorphic typing of error codes across features while keeping each enum scoped to
 * its own domain.
 *
 * <p>Frontends should use {@link #name()} as a stable key for internationalization (i18n), while
 * {@link #title()} provides a fallback English description for technical diagnostic.
 *
 * <p>Implementors are expected to be enums. The {@link #name()} method is declared explicitly so
 * that non-enum implementors are also required to provide a string identifier, keeping the contract
 * consistent regardless of the implementing type.
 */
public interface ErrorCode {

  /**
   * @return the unique identifier in UPPER_SNAKE_CASE (e.g., "NOT_FOUND").
   */
  @JsonValue
  String name();

  /**
   * This value provides a stable, high-level description suitable for presentation in API error
   * contracts or logging systems.
   *
   * @return a concise, human-readable summary of the error in English.
   */
  String title();
}
