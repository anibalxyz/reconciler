package com.anibalxyz.features.common.api.out;

public final class ErrorResponseExamples {

  public static final String INVALID_ID =
      """
            {
              "title": "Invalid or malformed request",
              "code": "BAD_REQUEST",
              "detail": "Invalid ID format. Must be a number."
            }""";

  public static final String RESOURCE_NOT_FOUND =
      """
            {
              "title": "The requested resource was not found",
              "code": "RESOURCE_NOT_FOUND",
              "detail": "User with id 1 not found"
            }""";

  public static final String UNAUTHORIZED =
      """
            {
              "title": "Access denied",
              "code": "UNAUTHORIZED",
              "detail": "Missing or invalid Authorization header"
            }""";

  public static final String INTERNAL_SERVER_ERROR =
      """
            {
              "title": "An internal server error occurred",
              "code": "INTERNAL_SERVER_ERROR"
            }""";

  private ErrorResponseExamples() {}
}
