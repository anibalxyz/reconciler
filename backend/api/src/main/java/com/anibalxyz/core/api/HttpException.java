package com.anibalxyz.core.api;

import com.anibalxyz.core.api.response.error.ErrorCode;
import com.anibalxyz.core.infra.InfrastructureException;

/**
 * Base class for all HTTP-layer exceptions.
 *
 * <p>Carries the three pieces of information the {@code ExceptionsMapper} needs to produce an
 * {@code ErrorResponse}: the HTTP status, the machine-readable {@link ErrorCode}, and the
 * user-facing detail message.
 */
public abstract class HttpException extends InfrastructureException {

  private final int status;
  private final ErrorCode errorCode;

  protected HttpException(int status, ErrorCode errorCode, String detail) {
    super(detail);
    this.status = status;
    this.errorCode = errorCode;
  }

  public int status() {
    return status;
  }

  public ErrorCode errorCode() {
    return errorCode;
  }

  public String detail() {
    return getMessage();
  }
}
