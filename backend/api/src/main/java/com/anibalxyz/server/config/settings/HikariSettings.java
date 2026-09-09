package com.anibalxyz.server.config.settings;

import com.anibalxyz.server.config.ConfigurationException;
import java.util.LinkedHashMap;
import java.util.Map;

public class HikariSettings implements Settings {
  private final int maxSize;
  private final int minIdleSize;
  private final long acquisitionTimeoutMillis;
  private final long validationTimeoutMillis;
  private final long initializationTimeoutMillis;
  private final long idleTimeoutMillis;

  private HikariSettings(
      int maxSize,
      int minIdleSize,
      long acquisitionTimeoutMillis,
      long validationTimeoutMillis,
      long initializationTimeoutMillis,
      long idleTimeoutMillis) {
    this.maxSize = maxSize;
    this.minIdleSize = minIdleSize;
    this.acquisitionTimeoutMillis = acquisitionTimeoutMillis;
    this.validationTimeoutMillis = validationTimeoutMillis;
    this.initializationTimeoutMillis = initializationTimeoutMillis;
    this.idleTimeoutMillis = idleTimeoutMillis;
  }

  public static HikariSettings from(SettingsReader reader) {
    int maxSize = Integer.parseInt(reader.read("HIKARI_MAX_SIZE"));
    if (maxSize < 1)
      throw new ConfigurationException.InvalidProperty("HIKARI_MAX_SIZE", "Must be at least 1");

    int minIdleSize = Integer.parseInt(reader.read("HIKARI_MIN_IDLE_SIZE"));
    if (minIdleSize < 0)
      throw new ConfigurationException.InvalidProperty(
          "HIKARI_MIN_IDLE_SIZE", "Must not be negative");

    long acquisitionTimeoutMillis = Long.parseLong(reader.read("HIKARI_ACQUISITION_TIMEOUT"));
    long validationTimeoutMillis = Long.parseLong(reader.read("HIKARI_VALIDATION_TIMEOUT"));
    long initializationTimeoutMillis = Long.parseLong(reader.read("HIKARI_INITIALIZATION_TIMEOUT"));
    long idleTimeoutMillis = Long.parseLong(reader.read("HIKARI_IDLE_TIMEOUT"));

    return new HikariSettings(
        maxSize,
        minIdleSize,
        acquisitionTimeoutMillis,
        validationTimeoutMillis,
        initializationTimeoutMillis,
        idleTimeoutMillis);
  }

  public int maxSize() {
    return maxSize;
  }

  public int minIdleSize() {
    return minIdleSize;
  }

  public long acquisitionTimeoutMillis() {
    return acquisitionTimeoutMillis;
  }

  public long validationTimeoutMillis() {
    return validationTimeoutMillis;
  }

  public long initializationTimeoutMillis() {
    return initializationTimeoutMillis;
  }

  public long idleTimeoutMillis() {
    return idleTimeoutMillis;
  }

  @Override
  public Map<String, Object> toMap() {
    Map<String, Object> configMap = new LinkedHashMap<>();
    configMap.put("hikari_max_size", maxSize);
    configMap.put("hikari_min_idle_size", minIdleSize);
    configMap.put("hikari_acquisition_timeout", acquisitionTimeoutMillis);
    configMap.put("hikari_validation_timeout", validationTimeoutMillis);
    configMap.put("hikari_initialization_timeout", initializationTimeoutMillis);
    configMap.put("hikari_idle_timeout", idleTimeoutMillis);
    return configMap;
  }

  @Override
  public String toString() {
    return toMap().toString();
  }
}
