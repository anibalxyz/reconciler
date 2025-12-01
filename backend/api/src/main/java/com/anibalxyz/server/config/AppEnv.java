package com.anibalxyz.server.config;

import java.util.Arrays;

// TODO: add a toString override or similar that converts to a lowercase string
public enum AppEnv {
  TEST,
  DEV,
  PROD;

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
