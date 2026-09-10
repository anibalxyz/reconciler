package com.anibalxyz.shared;

import com.anibalxyz.server.config.ConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

public class TestSettings {
  private final Map<String, String> settings;

  public TestSettings() {
    this.settings = new HashMap<>();
    var props = TestSettings.sourceFromClasspath();
    for (String key : props.stringPropertyNames()) {
      settings.put(key, props.getProperty(key));
    }
  }

  private static Properties sourceFromClasspath() {
    Properties props = new Properties();
    String propertiesFileName = "default.properties";
    try (InputStream in =
        TestSettings.class.getClassLoader().getResourceAsStream(propertiesFileName)) {
      if (in == null) {
        throw new IOException("Resource not found: " + propertiesFileName);
      }
      props.load(in);
    } catch (IOException e) {
      throw new ConfigurationException.UnableToLoadConfigurationFile(e);
    }
    return props;
  }

  public void override(Function<String, String> source) {
    for (String key : new HashSet<>(settings.keySet())) {
      String v = source.apply(key);
      if (v != null && !v.isBlank()) settings.put(key, v);
    }
  }

  public String get(String key) {
    return settings.get(key);
  }
}
