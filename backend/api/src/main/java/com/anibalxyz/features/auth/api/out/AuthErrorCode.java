package com.anibalxyz.features.auth.api.out;

import com.anibalxyz.features.common.api.out.code.ErrorCode;

public enum AuthErrorCode implements ErrorCode {
  REFRESH_TOKEN_NOT_FOUND("Refresh token not found"),
  REFRESH_TOKEN_EXPIRED("Refresh token expired"),
  ;

  private final String title;

  AuthErrorCode(String title) {
    this.title = title;
  }

  public String title() {
    return title;
  }
}
