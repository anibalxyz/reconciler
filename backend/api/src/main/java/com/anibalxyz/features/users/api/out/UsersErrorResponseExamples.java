package com.anibalxyz.features.users.api.out;

public final class UsersErrorResponseExamples {

  public static final String CREATE_USER_BAD_REQUEST =
      """
            {
              "title": "There was one or more validation errors",
              "code": "VALIDATION_ERROR",
              "type": "/api/errors/validation-error",
              "errors": [
                {
                  "code": "REQUIRED_FIELD",
                  "title": "This field is required",
                  "field": "name"
                }
              ]
            }""";

  public static final String UPDATE_USER_BAD_REQUEST =
      """
            {
              "title": "There was one or more validation errors",
              "code": "VALIDATION_ERROR",
              "detail": "At least one field (name, email, password) must be provided"
            }""";

  public static final String EMAIL_ALREADY_IN_USE =
      """
            {
              "title": "There was one or more validation errors",
              "code": "VALIDATION_ERROR",
              "type": "/api/errors/validation-error",
              "errors": [
                {
                  "code": "CONFLICT_FIELD",
                  "title": "This value is already in use",
                  "field": "email"
                }
              ]
            }""";

  private UsersErrorResponseExamples() {}
}
