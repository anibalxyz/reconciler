package com.anibalxyz.features.auth.api.exception;

import com.anibalxyz.core.api.exception.HttpException;
import com.anibalxyz.features.common.api.out.code.CommonErrorCode;

/**
 * Thrown by the JWT middleware when the {@code Authorization} header is missing or does not start
 * with the {@code Bearer } prefix.
 */
public final class MissingOrInvalidAuthHeader extends HttpException {

  public MissingOrInvalidAuthHeader() {
    super(401, CommonErrorCode.UNAUTHENTICATED, "Missing or invalid Authorization header");
  }
}
