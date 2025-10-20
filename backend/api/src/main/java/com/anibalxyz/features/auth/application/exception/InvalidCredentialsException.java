package com.anibalxyz.features.auth.application.exception;

import com.anibalxyz.features.common.application.exception.ApplicationException;

public class InvalidCredentialsException extends ApplicationException {
  public InvalidCredentialsException(String message) {
    super(message);
  }
}
