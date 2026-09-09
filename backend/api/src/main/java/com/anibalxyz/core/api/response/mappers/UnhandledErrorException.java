package com.anibalxyz.core.api.response.mappers;

import com.anibalxyz.core.primitives.ReconcilerException;

public class UnhandledErrorException extends ReconcilerException {
  public UnhandledErrorException(Object error) {
    super("Unhandled error type: " + error.getClass().getName());
  }
}
