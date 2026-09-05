package com.anibalxyz.features.common.api.out.code;

public enum CommonErrorCode implements ErrorCode {
  RESOURCE_NOT_FOUND("The requested resource was not found"),
  UNAUTHENTICATED("Authentication required"),
  ACCESS_DENIED("Access Denied"),
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
