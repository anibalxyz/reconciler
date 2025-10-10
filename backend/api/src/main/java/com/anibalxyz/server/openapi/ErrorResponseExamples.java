package com.anibalxyz.server.openapi;

public final class ErrorResponseExamples {

  public static final String INVALID_ID =
      """
            {
              "error": "Bad Request",
              "details": [
                "Invalid ID format. Must be a number."
              ]
            }""";
  public static final String USER_NOT_FOUND =
      """
            {
              "error": "Entity not found",
              "details": [
                "User with id 1 not found"
              ]
            }""";
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
  public static final String INTERNAL_SERVER_ERROR =
      """
            {
              "error": "Internal Server Error",
              "details": []
            }""";

  private ErrorResponseExamples() {}
}
