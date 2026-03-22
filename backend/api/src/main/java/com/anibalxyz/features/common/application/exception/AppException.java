package com.anibalxyz.features.common.application.exception;

public class AppException extends RuntimeException {
  private final Object error;

  public AppException(Object error) {
    this.error = error;
  }

  public Object getError() {
    return error;
  }

  @Override
  public synchronized Throwable fillInStackTrace() {
    return this;
  }
}
