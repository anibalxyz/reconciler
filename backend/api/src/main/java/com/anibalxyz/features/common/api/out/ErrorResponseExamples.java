package com.anibalxyz.features.common.api.out;

public final class ErrorResponseExamples {

  public static final String INVALID_ID =
      """
            {
              "error": "Bad Request",
              "details": [
                "Invalid ID format. Must be a number."
              ]
            }""";

  public static final String RESOURCE_NOT_FOUND =
      """
            {
              "error": "Resource not found",
              "details": [
                "Resource with id 1 not found"
              ]
            }""";

  public static final String UNAUTHORIZED =
      """
            {
              "error": "Unauthorized",
              "details": [
                "Missing or invalid Authorization header"
              ]
            }""";

  public static final String INTERNAL_SERVER_ERROR =
      """
            {
              "error": "Internal Server Error",
              "details": []
            }""";

  private ErrorResponseExamples() {}
}
