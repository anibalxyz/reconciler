package com.anibalxyz.features.common.api.out;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Marker interface for error code enums.
 *
 * <p>Enables polymorphic typing of error codes across features (e.g. {@code AuthErrorCode}, {@code
 * UserErrorCode}) while keeping each enum scoped to its own domain.
 *
 * <p>Implementors are expected to be enums. The {@link #name()} method is declared explicitly so
 * that non-enum implementors are also required to provide a string identifier, keeping the contract
 * consistent regardless of the implementing type.
 */
public interface ErrorCode {

  /**
   * Returns the string identifier for this error code.
   *
   * <p>For enums, this is provided automatically by {@link Enum#name()} and returns the constant
   * name as declared (e.g. {@code "SESSION_EXPIRED"}).
   *
   * <p>{@code @JsonValue} ensures Jackson always serializes this as a plain string, regardless of
   * whether the implementor is an enum or a regular class. Without it, Jackson might serialize the
   * object structure instead of the string identifier if the implementor is not an enum.
   *
   * @return the error code identifier in UPPER_SNAKE_CASE
   */
  @JsonValue
  String name();
}
