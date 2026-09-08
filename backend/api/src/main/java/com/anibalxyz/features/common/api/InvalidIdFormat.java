package com.anibalxyz.features.common.api;

import com.anibalxyz.core.api.HttpException;
import com.anibalxyz.core.api.response.error.CommonErrorCode;

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
