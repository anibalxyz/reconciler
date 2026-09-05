package com.anibalxyz.features.auth.api.exception;

import com.anibalxyz.core.api.exception.HttpException;
import com.anibalxyz.features.common.api.out.code.CommonErrorCode;

/** Thrown by the refresh-token handler when the request does not carry a refresh-token cookie. */
public final class MissingRefreshTokenCookie extends HttpException {

  public MissingRefreshTokenCookie() {
    super(401, CommonErrorCode.UNAUTHENTICATED, "Missing refresh token in cookie");
  }
}
