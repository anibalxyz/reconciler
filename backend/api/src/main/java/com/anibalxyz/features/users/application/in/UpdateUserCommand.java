package com.anibalxyz.features.users.application.in;

public record UpdateUserCommand(String name, String email, String password) {
  // TODO: move to a common place if reusable
  public enum FieldState {
    NOT_PRESENT,
    BLANK,
    VALID
  }

  public FieldState nameState() {
    if (name == null) return FieldState.NOT_PRESENT;
    if (name.isBlank()) return FieldState.BLANK;
    return FieldState.VALID;
  }

  public FieldState emailState() {
    if (email == null) return FieldState.NOT_PRESENT;
    if (email.isBlank()) return FieldState.BLANK;
    return FieldState.VALID;
  }

  public FieldState passwordState() {
    if (password == null) return FieldState.NOT_PRESENT;
    if (password.isBlank()) return FieldState.BLANK;
    return FieldState.VALID;
  }

  public boolean hasAtLeastOneField() {
    return nameState() == FieldState.VALID
        || emailState() == FieldState.VALID
        || passwordState() == FieldState.VALID;
  }
}
