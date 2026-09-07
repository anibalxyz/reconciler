package com.anibalxyz.server.config.environment;

import java.util.Map;

public class FeatureFlags implements ConfigGroup {
  private final boolean SWAGGER;

  private FeatureFlags(boolean swagger) {
    this.SWAGGER = swagger;
  }

  public static FeatureFlags from(ConfigValueReader reader) {
    String swaggerEnabledRaw = reader.read("FF_SWAGGER", true);
    if (swaggerEnabledRaw == null || swaggerEnabledRaw.isBlank()) swaggerEnabledRaw = "false";
    boolean swaggerEnabled = Boolean.parseBoolean(swaggerEnabledRaw);
    return new FeatureFlags(swaggerEnabled);
  }

  public boolean SWAGGER() {
    return SWAGGER;
  }

  @Override
  public Map<String, Object> toMap() {
    return Map.of("swagger", String.valueOf(SWAGGER));
  }

  @Override
  public String toString() {
    return toMap().toString();
  }
}
