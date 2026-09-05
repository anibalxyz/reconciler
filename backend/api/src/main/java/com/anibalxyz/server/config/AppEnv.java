package com.anibalxyz.server.config;

import com.anibalxyz.server.exception.ConfigurationException;
import java.util.Arrays;

// TODO: add a toString() override or similar that converts to a lowercase string
public enum AppEnv {
  TEST,
  DEV,
  PROD;

  /**
   * Parses a string value into an `AppEnv` enum. The comparison is case-insensitive.
   *
   * @param value The string value to parse (e.g., "dev", "prod", "test").
   * @return The corresponding `AppEnv` enum.
   * @throws ConfigurationException.NeededPropertyException if the value is null, blank, or does not
   *     match any valid environment.
   */
  public static AppEnv parseFromString(String value) {
    if (value == null || value.isBlank()) {
      throw new ConfigurationException.MissingProperty("APP_ENV");
    }
    try {
      return AppEnv.valueOf(value.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new ConfigurationException.InvalidProperty(
          "APP_ENV", "Available values are: " + Arrays.toString(AppEnv.values()));
    }
  }
}
