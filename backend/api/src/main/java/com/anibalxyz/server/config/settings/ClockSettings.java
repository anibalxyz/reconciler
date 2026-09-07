package com.anibalxyz.server.config.settings;

import com.anibalxyz.server.config.SettingsReader;
import java.time.Instant;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

public class ClockSettings implements Settings {
  private final ZoneId systemTimezone;
  private final Instant systemTimeOverride;

  private ClockSettings(ZoneId systemTimezone, Instant systemTimeOverride) {
    this.systemTimezone = systemTimezone;
    this.systemTimeOverride = systemTimeOverride;
  }

  public static ClockSettings from(SettingsReader reader) {
    ZoneId systemTimezone = ZoneId.of(reader.read("SYSTEM_TIMEZONE"));
    String systemTimeOverrideString = reader.read("SYSTEM_TIME_OVERRIDE", true);
    Instant systemTimeOverride =
        (systemTimeOverrideString == null || systemTimeOverrideString.isBlank())
            ? null
            : Instant.parse(systemTimeOverrideString);

    return new ClockSettings(systemTimezone, systemTimeOverride);
  }

  public ZoneId systemTimezone() {
    return systemTimezone;
  }

  public Instant systemTimeOverride() {
    return systemTimeOverride;
  }

  @Override
  public Map<String, Object> toMap() {
    Map<String, Object> configMap = new LinkedHashMap<>();
    configMap.put("system_timezone", systemTimezone.toString());
    configMap.put("system_time_override", systemTimeOverride);
    return configMap;
  }

  @Override
  public String toString() {
    return toMap().toString();
  }
}
