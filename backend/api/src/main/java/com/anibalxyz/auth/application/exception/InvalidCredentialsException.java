package com.anibalxyz.auth.application.exception;

import com.anibalxyz.application.exception.ApplicationException;

public class InvalidCredentialsException extends ApplicationException {
  public InvalidCredentialsException(String message) {
    super(message);
  }
}
