package com.anibalxyz.features.users.application.in;

import com.anibalxyz.features.common.application.ValidationNotification;

public record CreateUserCommand(String name, String email, String password) {

  public void validate(ValidationNotification notification) {
    if (name == null || name.isBlank()) notification.addMissing("name");
    if (email == null || email.isBlank()) notification.addMissing("email");
    if (password == null || password.isBlank()) notification.addMissing("password");
  }
}
