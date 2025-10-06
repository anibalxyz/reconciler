package com.anibalxyz.users.domain.exception;

public class InvalidPasswordFormatException extends IllegalArgumentException {
  public InvalidPasswordFormatException(String message) {
    super(message);
  }
}