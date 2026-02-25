package com.anibalxyz.features.common.api.out;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
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
 *       #extensions()}
 * </ul>
 *
 * <p>This class is a record, ensuring structural immutability. Each fluent method returns a new
 * instance with the change applied, preserving immutability across the construction chain.
 *
 * <p>The {@code code} field is stored internally as a {@code String} to enable native Jackson
 * deserialization without custom deserializers. The public API accepts {@link ErrorCode} instances
 * only, ensuring type safety at construction time while remaining transparent to callers.
 *
 * <p>The {@code extensions} map is mutable during Jackson deserialization (unknown fields are
 * collected into it via {@code @JsonAnySetter}), but exposed as an unmodifiable view via {@link
 * #extensions()} to prevent external mutation.
 *
 * <p>Usage examples:
 *
 * <pre>{@code
 * // Simple error
 * new ErrorResponse(AuthErrorCode.SESSION_EXPIRED, "Authentication failed")
 *     .type("/api/errors/authentication-error")
 *     .detail("Your session expired 2 hours ago. Please log in again.");
 *
 * // Error with sub-errors
 * new ErrorResponse(CommonErrorCode.VALIDATION_ERROR, "The provided data contains validation errors")
 *     .type("/api/errors/validation-error")
 *     .errors(List.of(
 *         new ErrorDetail(ValidationErrorCode.PASSWORD_TOO_SHORT)
 *             .with("field", "#/password")
 *             .with("detail", "Password must be at least 8 characters."),
 *         new ErrorDetail(ValidationErrorCode.EMAIL_INVALID_FORMAT)
 *             .with("field", "#/email")
 *             .with("detail", "Email format is invalid.")
 *     ));
 *
 * // Error with custom extension fields
 * new ErrorResponse(AuthErrorCode.INSUFFICIENT_PERMISSIONS, "You don't have permission to perform this action")
 *     .type("/api/errors/authorization-error")
 *     .with("requiredRole", "ADMIN");
 * }</pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    String type,
    String title,
    String detail,
    String code,
    String instance,
    List<ErrorDetail> errors,
    @JsonAnySetter Map<String, Object> extensions) {

  /**
   * Factory method used exclusively by Jackson for deserialization.
   *
   * <p>Accepts {@code code} as a {@code String} to enable native Jackson deserialization without
   * custom deserializers. Unknown JSON fields are collected into {@code extensions} via
   * {@code @JsonAnySetter}.
   *
   * <p>Not intended for direct use — use {@link #ErrorResponse(ErrorCode, String)} instead.
   *
   * @param type URI identifying the error category
   * @param title human-readable summary of the problem type
   * @param detail occurrence-specific explanation
   * @param code machine-readable error identifier as a plain string
   * @param instance unique request identifier
   * @param errors list of sub-errors
   * @param extensions map of additional fields collected during deserialization
   * @return a new {@code ErrorResponse} instance
   */
  @JsonCreator
  public static ErrorResponse create(
      @JsonProperty("type") String type,
      @JsonProperty("title") String title,
      @JsonProperty("detail") String detail,
      @JsonProperty("code") String code,
      @JsonProperty("instance") String instance,
      @JsonProperty("errors") List<ErrorDetail> errors,
      @JsonAnySetter Map<String, Object> extensions) {
    return new ErrorResponse(
        type,
        title,
        detail,
        code,
        instance,
        errors,
        extensions != null ? extensions : new LinkedHashMap<>());
  }

  /**
   * Creates a new {@code ErrorResponse} with the required fields and no optional data.
   *
   * @param code machine-readable error identifier — must be a registered {@link ErrorCode}
   * @param title human-readable summary of the problem type
   */
  public ErrorResponse(ErrorCode code, String title) {
    this(null, title, null, code.name(), null, null, new LinkedHashMap<>());
  }

  /**
   * Creates a new {@code ErrorResponse} inferring the {@code title} from the given {@link
   * ErrorCode}.
   *
   * <p>Prefer this constructor when the error code's title is sufficient as the response summary,
   * avoiding redundant repetition of the same string at the call site.
   *
   * @param code machine-readable error identifier — must be a registered {@link ErrorCode}
   */
  public ErrorResponse(ErrorCode code) {
    this(null, code.title(), null, code.name(), null, null, new LinkedHashMap<>());
  }

  /**
   * Returns the extension fields, serialized as top-level JSON properties.
   *
   * <p>{@code @JsonAnyGetter} instructs Jackson to flatten the map entries as root-level properties
   * during serialization (e.g. {@code "requiredRole": "ADMIN"} instead of {@code "extensions":
   * {"requiredRole": "ADMIN"}}).
   *
   * <p>Returns {@code null} when empty so that {@code @JsonInclude(NON_NULL)} omits it from the
   * serialized output.
   *
   * @return unmodifiable view of the extension fields, or {@code null} if empty
   */
  @Override
  @JsonAnyGetter
  public Map<String, Object> extensions() {
    if (extensions == null || extensions.isEmpty()) return null;
    return Collections.unmodifiableMap(extensions);
  }

  /**
   * Returns a new instance with the given {@code type} URI.
   *
   * @param type URI identifying the error category (e.g. {@code /api/errors/validation-error})
   * @return a new {@code ErrorResponse} with the type set
   */
  public ErrorResponse type(String type) {
    return new ErrorResponse(
        type, title, detail, code, instance, errors, new LinkedHashMap<>(extensions));
  }

  /**
   * Returns a new instance with the given occurrence-specific {@code detail} message.
   *
   * @param detail human-readable explanation specific to this occurrence
   * @return a new {@code ErrorResponse} with the detail set
   */
  public ErrorResponse detail(String detail) {
    return new ErrorResponse(
        type, title, detail, code, instance, errors, new LinkedHashMap<>(extensions));
  }

  /**
   * Returns a new instance with the given {@code instance} request identifier.
   *
   * @param instance unique request identifier (e.g. {@code req-<UUID>})
   * @return a new {@code ErrorResponse} with the instance set
   */
  public ErrorResponse instance(String instance) {
    return new ErrorResponse(
        type, title, detail, code, instance, errors, new LinkedHashMap<>(extensions));
  }

  /**
   * Returns a new instance with the given list of sub-errors.
   *
   * @param errors list of {@link ErrorDetail} objects describing each sub-error
   * @return a new {@code ErrorResponse} with the errors set
   */
  public ErrorResponse errors(List<ErrorDetail> errors) {
    return new ErrorResponse(
        type, title, detail, code, instance, errors, new LinkedHashMap<>(extensions));
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
}
