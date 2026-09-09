package com.anibalxyz.features.auth.api.exception;

import com.anibalxyz.core.api.HttpException;
import com.anibalxyz.core.api.response.error.CommonErrorCode;

/** Thrown by the refresh-token handler when the request does not carry a refresh-token cookie. */
public final class MissingRefreshTokenCookie extends HttpException {

  public MissingRefreshTokenCookie() {
    super(401, CommonErrorCode.UNAUTHENTICATED, "Missing refresh token in cookie");
  }
}
