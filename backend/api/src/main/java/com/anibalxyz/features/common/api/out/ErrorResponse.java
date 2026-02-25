package com.anibalxyz.features.common.api.out;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RFC 9457-compliant error response DTO with practical extensions.
 *
 * <p>Standard RFC 9457 fields:
 *
 * <ul>
 *   <li>{@code type} (optional): URI identifying the error category (e.g. {@code
 *       /api/errors/authentication-error})
 *   <li>{@code title} (required): human-readable summary, stable across occurrences
 *   <li>{@code detail} (optional): occurrence-specific explanation
 *   <li>{@code instance} (optional): unique request identifier (e.g. {@code req-<UUID>})
 * </ul>
 *
 * <p>Extension fields:
 *
 * <ul>
 *   <li>{@code code} (required): machine-readable identifier in UPPER_SNAKE_CASE
 *   <li>{@code errors} (optional): list of sub-errors (e.g. validation failures)
 *   <li>Additional fields (optional): serialized as top-level JSON properties via {@link
 *       #getExtensions()}
 * </ul>
 *
 * <p>Instances are immutable. Each method returns a new instance with the change applied.
 *
 * <p>Usage examples:
 *
 * <pre>{@code
 * // Simple error
 * ErrorResponse error = new ErrorResponse("SESSION_EXPIRED", "Authentication failed")
 *     .type("/api/errors/authentication-error")
 *     .detail("Your session expired 2 hours ago. Please log in again.");
 *
 * // Error with sub-errors
 * ErrorResponse error = new ErrorResponse("VALIDATION_ERROR", "The provided data contains validation errors")
 *     .type("/api/errors/validation-error")
 *     .errors(List.of(
 *         new ErrorDetail("PASSWORD_TOO_SHORT").with("field", "#/password").with("detail", "Password must be at least 8 characters."),
 *         new ErrorDetail("EMAIL_INVALID_FORMAT").with("field", "#/email").with("detail", "Email format is invalid.")
 *     ));
 *
 * // Error with custom extension fields
 * ErrorResponse error = new ErrorResponse("INSUFFICIENT_PERMISSIONS", "You don't have permission to perform this action")
 *     .type("/api/errors/authorization-error")
 *     .with("requiredRole", "ADMIN");
 * }</pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

  private final String type;
  private final String title;
  private final String detail;
  private final ErrorCode code;
  private final String instance;
  private final List<ErrorDetail> errors;
  private final Map<String, Object> extensions;

  /**
   * Creates a new {@code ErrorResponse} with the required fields and no optional data.
   *
   * @param code machine-readable error identifier in UPPER_SNAKE_CASE
   * @param title human-readable summary of the problem type
   */
  public ErrorResponse(ErrorCode code, String title) {
    this.title = title;
    this.code = code;
    this.type = null;
    this.detail = null;
    this.instance = null;
    this.errors = null;
    this.extensions = new LinkedHashMap<>();
  }

  private ErrorResponse(
      String type,
      String title,
      String detail,
      ErrorCode code,
      String instance,
      List<ErrorDetail> errors,
      Map<String, Object> extensions) {
    this.type = type;
    this.title = title;
    this.detail = detail;
    this.code = code;
    this.instance = instance;
    this.errors = errors;
    this.extensions = extensions;
  }

  /**
   * Returns a new instance with the given {@code type} URI.
   *
   * @param type URI identifying the error category (e.g. {@code /api/errors/validation-error})
   * @return a new {@code ErrorResponse} with the type set
   */
  public ErrorResponse type(String type) {
    return new ErrorResponse(type, title, detail, code, instance, errors, extensions);
  }

  /**
   * Returns a new instance with the given occurrence-specific {@code detail} message.
   *
   * @param detail human-readable explanation specific to this occurrence
   * @return a new {@code ErrorResponse} with the detail set
   */
  public ErrorResponse detail(String detail) {
    return new ErrorResponse(type, title, detail, code, instance, errors, extensions);
  }

  /**
   * Returns a new instance with the given {@code instance} request identifier.
   *
   * @param instance unique request identifier (e.g. {@code req-<UUID>})
   * @return a new {@code ErrorResponse} with the instance set
   */
  public ErrorResponse instance(String instance) {
    return new ErrorResponse(type, title, detail, code, instance, errors, extensions);
  }

  /**
   * Returns a new instance with the given list of sub-errors.
   *
   * @param errors list of {@link ErrorDetail} objects describing each sub-error
   * @return a new {@code ErrorResponse} with the errors set
   */
  public ErrorResponse errors(List<ErrorDetail> errors) {
    return new ErrorResponse(type, title, detail, code, instance, errors, extensions);
  }

  /**
   * Returns a new instance with the given key-value pair added to the extension fields.
   *
   * <p>Extension fields are serialized as top-level JSON properties (e.g. {@code requiredRole},
   * {@code retryAfter}).
   *
   * @param key the field name to include in the JSON output
   * @param value the field value
   * @return a new {@code ErrorResponse} with the extension added
   */
  public ErrorResponse with(String key, Object value) {
    Map<String, Object> updated = new LinkedHashMap<>(extensions);
    updated.put(key, value);
    return new ErrorResponse(type, title, detail, code, instance, errors, updated);
  }

  /**
   * @return the error category URI
   */
  public String getType() {
    return type;
  }

  /**
   * @return the human-readable summary
   */
  public String getTitle() {
    return title;
  }

  /**
   * @return the occurrence-specific detail message
   */
  public String getDetail() {
    return detail;
  }

  /**
   * @return the machine-readable error code
   */
  public ErrorCode getCode() {
    return code;
  }

  /**
   * @return the unique request identifier
   */
  public String getInstance() {
    return instance;
  }

  /**
   * @return the list of sub-errors
   */
  public List<ErrorDetail> getErrors() {
    return errors;
  }

  /**
   * Returns the extension fields, serialized as top-level JSON properties.
   *
   * @return map of additional fields, or empty map if none
   */
  @JsonAnyGetter
  public Map<String, Object> getExtensions() {
    return extensions.isEmpty() ? null : extensions;
  }
}
