package com.anibalxyz.features.auth.api.exception;

import com.anibalxyz.core.api.HttpException;
import com.anibalxyz.core.api.response.error.CommonErrorCode;

/**
 * Thrown by the JWT middleware when an authenticated user does not have any role required by the
 * route.
 */
public final class AccessDenied extends HttpException {

  public AccessDenied() {
    super(403, CommonErrorCode.ACCESS_DENIED, "Access denied");
  }
}
