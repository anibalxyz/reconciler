package com.anibalxyz.server.config.environment;

import com.anibalxyz.server.exception.ConfigurationException;
import java.util.Optional;

public class ArgParser {
  public static Optional<String> find(String option, String[] args) {
    for (int i = 0; i < args.length; i++) {
      if (option.equals(args[i])) {
        if (i + 1 < args.length) {
          return Optional.of(args[i + 1]);
        }
        throw new ConfigurationException.InvalidProperty(option, "Expected an argument value");
      }
    }
    return Optional.empty();
  }
}
