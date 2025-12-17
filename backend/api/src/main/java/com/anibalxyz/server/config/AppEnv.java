package com.anibalxyz.server.config;

import java.util.Arrays;

// TODO: add a toString() override or similar that converts to a lowercase string
/** Defines the different application environments. */
public enum AppEnv {
  /** Test environment. */
  TEST,
  /** Development environment. */
  DEV,
  /** Production environment. */
  PROD;

  /**
   * Parses a string value into an `AppEnv` enum. The comparison is case-insensitive.
   *
   * @param value The string value to parse (e.g., "dev", "prod", "test").
   * @return The corresponding `AppEnv` enum.
   * @throws IllegalStateException if the value is null, blank, or does not match any valid
   *     environment.
   */
  public static AppEnv parseFromString(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalStateException("APP_ENV cannot be null or blank");
    }
    try {
      return AppEnv.valueOf(value.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException(
          "Invalid APP_ENV value '"
              + value
              + "'. Available values: "
              + Arrays.toString(AppEnv.values()),
          e);
    }
  }
}
