package com.anibalxyz.features.auth.api.exception;

import com.anibalxyz.core.api.exception.HttpException;
import com.anibalxyz.features.common.api.out.code.CommonErrorCode;

/**
 * Thrown by the JWT middleware when an authenticated user does not have any role required by the
 * route.
 */
public final class AccessDenied extends HttpException {

  public AccessDenied() {
    super(403, CommonErrorCode.UNAUTHORIZED, "Access denied");
  }
}
