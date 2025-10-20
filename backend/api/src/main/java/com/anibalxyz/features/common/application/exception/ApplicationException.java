package com.anibalxyz.features.common.application.exception;

public abstract class ApplicationException extends RuntimeException {
  public ApplicationException(String message) {
    super(message);
  }
}
