package com.anibalxyz.server.exception;

import com.anibalxyz.reconciler.exception.ReconcilerException;

public class UnregisteredMapperException extends ReconcilerException {
  public UnregisteredMapperException(Object error) {
    super("No mapper registered for error type: " + error.getClass().getName());
  }
}
