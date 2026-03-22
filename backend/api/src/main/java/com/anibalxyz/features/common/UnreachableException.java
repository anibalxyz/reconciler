package com.anibalxyz.features.common;

public class UnreachableException extends RuntimeException {

  public UnreachableException(String message) {
    super(message);
  }

  public static UnreachableException of(Object unexpected) {
    return new UnreachableException(
        "Should never happen: " + unexpected.getClass().getSimpleName());
  }
}
