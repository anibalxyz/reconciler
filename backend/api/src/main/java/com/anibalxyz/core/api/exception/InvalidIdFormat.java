package com.anibalxyz.core.api.exception;

import com.anibalxyz.features.common.api.out.code.CommonErrorCode;

/** Thrown when a request path parameter expected to be a numeric ID cannot be parsed as such. */
public final class InvalidIdFormat extends HttpException {

  private final String rawValue;

  public InvalidIdFormat(String rawValue) {
    super(400, CommonErrorCode.BAD_REQUEST, "Invalid ID format. Must be a number.");
    this.rawValue = rawValue;
  }

  public String rawValue() {
    return rawValue;
  }
}
