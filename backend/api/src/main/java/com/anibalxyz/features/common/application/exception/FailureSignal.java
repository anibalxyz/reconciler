package com.anibalxyz.features.common.application.exception;

public class FailureSignal extends RuntimeException {
  private final Object error;

  public FailureSignal(Object error) {
    // This makes the exception extremely lightweight by disabling stack trace generation.
    super(null, null, false, false);
    this.error = error;
  }

  public Object getError() {
    return error;
  }
}
