package com.anibalxyz.core.infra.exception;

import com.anibalxyz.reconciler.exception.ReconcilerException;

/**
 * Marker base for exceptions raised by cross-cutting infrastructure concerns (e.g. the JJWT
 * library, JSON serialization).
 */
public abstract class InfrastructureException extends ReconcilerException {

  protected InfrastructureException(String message) {
    super(message);
  }

  protected InfrastructureException(String message, Throwable cause) {
    super(message, cause);
  }
}
