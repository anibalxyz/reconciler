package com.anibalxyz.users.application.exception;

public class EntityNotFoundException extends RuntimeException {
  public EntityNotFoundException(String message) {
    super(message);
  }

  public EntityNotFoundException() {
    super();
  }
}
