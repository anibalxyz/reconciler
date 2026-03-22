package com.anibalxyz.features.common.api.out;

/**
 * Error codes for field-level validation failures.
 *
 * <p>identify the specific validation rule that failed. Each code carries a human-readable {@link
 * #title()} for logging and debugging purposes.
 *
 * <p>Frontends must use the {@code code} value for localized user-facing messages.
 */
public enum ValidationErrorCode implements ErrorCode {
  VALIDATION_ERROR("There was one or more validation errors"),
  REQUIRED_FIELD("This field is required"),
  BLANK_FIELD("This field cannot be blank"),
  CONFLICT_FIELD("This value is already in use"),
  INVALID_EMAIL_FORMAT("Invalid email format"),
  INVALID_PASSWORD_EMPTY("Password cannot be empty"),
  INVALID_PASSWORD_TOO_SHORT("Password too short"),
  INVALID_PASSWORD_TOO_LONG("Password too long");

  private final String title;

  ValidationErrorCode(String title) {
    this.title = title;
  }

  @Override
  public String title() {
    return title;
  }
}
