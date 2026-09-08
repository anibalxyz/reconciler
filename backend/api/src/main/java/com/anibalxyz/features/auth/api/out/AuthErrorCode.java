package com.anibalxyz.features.auth.api.out;

import com.anibalxyz.core.api.response.error.ErrorCode;

public enum AuthErrorCode implements ErrorCode {
  REFRESH_TOKEN_NOT_FOUND("Refresh token not found"),
  REFRESH_TOKEN_EXPIRED("Refresh token expired"),
  REFRESH_TOKEN_INVALID("Refresh token invalid");

  private final String title;

  AuthErrorCode(String title) {
    this.title = title;
  }

  public String title() {
    return title;
  }
}
