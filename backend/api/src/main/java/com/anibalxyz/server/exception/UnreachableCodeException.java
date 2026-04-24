package com.anibalxyz.server.exception;

import com.anibalxyz.annotation.ExcludeFromJacocoGenerated;

@ExcludeFromJacocoGenerated
public final class UnreachableCodeException extends IllegalStateException {

  private UnreachableCodeException(String message) {
    super(message);
  }

  public static UnreachableCodeException of(Object unexpected) {
    return new UnreachableCodeException(formatMessage(unexpected, null));
  }

  public static UnreachableCodeException of(Object unexpected, String reason) {
    return new UnreachableCodeException(formatMessage(unexpected, reason));
  }

  private static String formatMessage(Object unexpected, String reason) {
    String msg = "This code should never be reached. Got: " + unexpected.getClass().getName();
    return (reason == null) ? msg : msg + ". Reason: " + reason;
  }
}
