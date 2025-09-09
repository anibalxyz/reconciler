package com.anibalxyz.users.api.in;

import com.anibalxyz.users.application.in.UserUpdatePayload;

public record UserUpdateRequest(String name, String email, String password)
    implements UserUpdatePayload {

  public boolean isValid() {
    return !((name == null || name.isBlank())
        && (email == null || email.isBlank())
        && (password == null || password.isBlank()));
  }
}
