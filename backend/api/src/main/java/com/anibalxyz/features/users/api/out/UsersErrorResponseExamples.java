package com.anibalxyz.features.users.api.out;

public final class UsersErrorResponseExamples {

  public static final String CREATE_USER_BAD_REQUEST =
      """
            {
              "error": "Invalid input provided",
              "details": [
                "Name is required"
              ]
            }""";

  public static final String UPDATE_USER_BAD_REQUEST =
      """
            {
              "error": "Invalid input provided",
              "details": [
                "At least one field (name, email, password) must be provided"
              ]
            }""";

  public static final String EMAIL_ALREADY_IN_USE =
      """
            {
              "error": "Conflict",
              "details": [
                "Email already in use. Please use another"
              ]
            }""";

  private UsersErrorResponseExamples() {}
}
