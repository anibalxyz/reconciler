package com.anibalxyz.features.auth.api.out;

public final class AuthErrorResponseExamples {

  public static final String INVALID_CREDENTIALS =
      """
            {
              "title": "Access denied",
              "code": "UNAUTHORIZED",
              "detail": "Invalid credentials"
            }""";

  public static final String MISSING_REFRESH_TOKEN =
      """
            {
              "title": "Access denied",
              "code": "UNAUTHORIZED",
              "detail": "Missing refresh token in cookie"
            }""";

  public static final String INVALID_INPUT_PROVIDED =
      """
            {
              "title": "There was one or more validation errors",
              "code": "VALIDATION_ERROR",
              "type": "/api/errors/validation-error",
              "errors": [
                {
                  "code": "INVALID_FIELD_FORMAT",
                  "title": "Invalid field format",
                  "field": "email"
                }
              ]
            }""";

  private AuthErrorResponseExamples() {}
}
