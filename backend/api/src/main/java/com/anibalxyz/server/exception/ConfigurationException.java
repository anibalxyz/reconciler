package com.anibalxyz.server.exception;

import com.anibalxyz.reconciler.exception.ReconcilerException;
import java.io.IOException;

public class ConfigurationException extends ReconcilerException {
  public ConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }

  public static class UnableToLoadConfigurationFile extends ConfigurationException {
    public UnableToLoadConfigurationFile(IOException e) {
      super("Could not load configuration file", e);
    }
  }

  public abstract static class NeededPropertyException extends ConfigurationException {
    public NeededPropertyException(String message) {
      super(message, null);
    }
  }

  public static class MissingProperty extends NeededPropertyException {
    public MissingProperty(String propertyName) {
      super("Missing required property: " + propertyName);
    }
  }

  public static class InvalidProperty extends NeededPropertyException {
    public InvalidProperty(String propertyName, String reason) {
      super("Invalid " + propertyName + ". Reason: " + reason);
    }
  }
}
