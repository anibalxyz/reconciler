package com.anibalxyz.server.exception;

public class UnregisteredMapperException extends IllegalStateException {
  public UnregisteredMapperException(Object error) {
    super("No mapper registered for error type: " + error.getClass().getName());
  }
}
