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
  // TODO: check hierarchy management
  VALIDATION_ERROR("There was one or more validation errors"),
  REQUIRED_FIELD("This field is required"),
  BLANK_FIELD("This field cannot be blank"),
  CONFLICT_FIELD("This value is already in use"),
  INVALID_FIELD_FORMAT("Invalid field format"),
  TOO_SHORT("This field is too short"),
  TOO_LONG("This field is too long");

  private final String title;

  ValidationErrorCode(String title) {
    this.title = title;
  }

  @Override
  public String title() {
    return title;
  }
}
