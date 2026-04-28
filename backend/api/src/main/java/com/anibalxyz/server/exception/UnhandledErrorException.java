package com.anibalxyz.server.exception;

public class UnhandledErrorException extends RuntimeException {
  public UnhandledErrorException(Object error) {
    super("Unhandled error type: " + error.getClass().getName());
  }
}
