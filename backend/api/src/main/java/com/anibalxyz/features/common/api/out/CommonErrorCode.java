package com.anibalxyz.features.common.api.out;

public enum CommonErrorCode implements ErrorCode {
  VALIDATION_ERROR("The provided data contains validation errors"),
  RESOURCE_NOT_FOUND("The requested resource was not found");

  private final String title;

  public String title() {
    return title;
  }

  CommonErrorCode(String title) {
    this.title = title;
  }
}
