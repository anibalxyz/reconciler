package com.anibalxyz.features.common.api.out;

public enum CommonErrorCode implements ErrorCode {
  VALIDATION_ERROR("The provided data contains validation errors"),
  RESOURCE_NOT_FOUND("The requested resource was not found"),
  UNAUTHORIZED("Auth access"),
  INTERNAL_SERVER_ERROR("An internal server error occurred"),
  UNAVAILABLE_SERVICE("Service temporarily unavailable"),
  BAD_REQUEST("Invalid or malformed request");

  private final String title;

  CommonErrorCode(String title) {
    this.title = title;
  }

  public String title() {
    return title;
  }
}
