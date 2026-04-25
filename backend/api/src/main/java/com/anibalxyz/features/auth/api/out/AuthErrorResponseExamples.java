package com.anibalxyz.features.auth.api.out;

public final class AuthErrorResponseExamples {

  public static final String INVALID_CREDENTIALS =
      """
            {
              "error": "Invalid credentials",
              "details": []
            }""";

  public static final String MISSING_REFRESH_TOKEN =
      """
            {
              "error": "Unauthorized",
              "details": [
                "Missing refresh token in cookie"
              ]
            }""";

  public static final String INVALID_INPUT_PROVIDED =
      """
                {
                  "error": "Invalid input provided",
                  "details": [
                    "Invalid email provided."
                  ]
                }""";

  private AuthErrorResponseExamples() {}
}
