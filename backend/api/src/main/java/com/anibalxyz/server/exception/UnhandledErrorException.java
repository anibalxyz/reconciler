package com.anibalxyz.server.exception;

import com.anibalxyz.reconciler.exception.ReconcilerException;

public class UnhandledErrorException extends ReconcilerException {
  public UnhandledErrorException(Object error) {
    super("Unhandled error type: " + error.getClass().getName());
  }
}
