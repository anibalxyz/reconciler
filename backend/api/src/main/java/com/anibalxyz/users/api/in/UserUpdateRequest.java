package com.anibalxyz.users.api.in;

import com.anibalxyz.users.application.in.UserUpdatePayload;

public record UserUpdateRequest(String name, String email, String password)
    implements UserUpdatePayload {

  public boolean isValid() {
    return !((this.name == null || this.name.isBlank())
        && (this.email == null || this.email.isBlank())
        && (this.password == null || this.password.isBlank()));
  }
}
