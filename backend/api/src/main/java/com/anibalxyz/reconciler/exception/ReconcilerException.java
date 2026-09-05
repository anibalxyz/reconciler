package com.anibalxyz.reconciler.exception;

/**
 * Root of the project's custom exception hierarchy.
 *
 * <p>All custom exceptions raised by any layer of the system should extend this class (directly or
 * transitively).
 */
public abstract class ReconcilerException extends RuntimeException {

  protected ReconcilerException(String message) {
    super(message);
  }

  protected ReconcilerException(String message, Throwable cause) {
    super(message, cause);
  }
}
