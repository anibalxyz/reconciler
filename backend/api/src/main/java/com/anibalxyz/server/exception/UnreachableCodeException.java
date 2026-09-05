package com.anibalxyz.server.exception;

import com.anibalxyz.annotation.ExcludeFromJacocoGenerated;
import com.anibalxyz.reconciler.exception.ReconcilerException;

@ExcludeFromJacocoGenerated
public final class UnreachableCodeException extends ReconcilerException {

  private UnreachableCodeException(String message) {
    super(message);
  }

  public static UnreachableCodeException of(Object unexpected) {
    return of(unexpected, null);
  }

  public static UnreachableCodeException of(Object unexpected, String reason) {
    return new UnreachableCodeException(formatMessage(unexpected, reason));
  }

  public static UnreachableCodeException of(Throwable cause, String reason) {
    UnreachableCodeException ex = new UnreachableCodeException(formatMessage(cause, reason));
    ex.initCause(cause);
    return ex;
  }

  private static String formatMessage(Object unexpected, String reason) {
    String msg = "This code should never be reached. Got: " + unexpected.getClass().getName();
    return (reason == null) ? msg : msg + ". Reason: " + reason;
  }
}
