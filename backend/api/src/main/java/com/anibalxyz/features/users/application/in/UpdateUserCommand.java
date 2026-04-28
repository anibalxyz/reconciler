package com.anibalxyz.features.users.application.in;

public record UpdateUserCommand(String name, String email, String password) {
  public boolean hasAtLeastOneField() {
    return name != null || email != null || password != null;
  }
}
