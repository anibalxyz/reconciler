package com.anibalxyz.server.config;

import com.anibalxyz.server.exception.ConfigurationException;
import java.util.function.Function;

/**
 * Reads config values from the given sourcing method (e.g. {@code System::getenv}) without knowing
 * which one it is.
 */
@FunctionalInterface
public interface ConfigValueReader {
  /**
   * Adapts a name-to-value source (e.g. {@code System::getenv}) into a reader.
   *
   * @param source resolves a variable name to its raw value
   * @return a reader throwing {@link ConfigurationException.MissingProperty} for missing or blank
   *     required values
   */
  static ConfigValueReader from(Function<String, String> source)
      throws ConfigurationException.MissingProperty {
    return (name, allowEmpty) -> {
      String value = source.apply(name);
      if (!allowEmpty && (value == null || value.isBlank())) {
        throw new ConfigurationException.MissingProperty(name);
      }
      return value;
    };
  }

  /**
   * Reads a variable.
   *
   * @param name the variable name (e.g. {@code APP_ENV})
   * @param allowEmpty allow missing or blank values
   * @return the raw value. Null or blank when {@code allowEmpty} is true
   * @throws ConfigurationException.MissingProperty if the value is missing or blank and {@code
   *     allowEmpty} is false
   */
  String read(String name, boolean allowEmpty) throws ConfigurationException.MissingProperty;

  /**
   * Overload of {@link #read(String, boolean)} to read a required variable.
   *
   * @param name the variable name
   * @return the non-blank value
   * @throws ConfigurationException.MissingProperty if the value is missing or blank
   */
  default String read(String name) {
    return read(name, false);
  }
}
