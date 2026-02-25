package com.anibalxyz.features.common.api.out;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a single entry in the {@code errors} array of an {@link ErrorResponse}.
 *
 * <p>Each detail has a required {@code code} (machine-readable identifier) and optional additional
 * fields serialized as top-level JSON properties via {@link #getExtensions()}.
 *
 * <p>Instances are immutable. Each method returns a new instance with the change applied.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * ErrorDetail detail = new ErrorDetail("PASSWORD_TOO_SHORT")
 *     .field("#/password")
 *     .detail("Password must be at least 8 characters.");
 * }</pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorDetail {

  private final ErrorCode code;
  private final Map<String, Object> extensions;

  /**
   * Creates a new {@code ErrorDetail} with the given error code and no additional fields.
   *
   * @param code machine-readable error identifier in UPPER_SNAKE_CASE
   */
  public ErrorDetail(ErrorCode code) {
    this.code = code;
    this.extensions = new LinkedHashMap<>();
  }

  private ErrorDetail(ErrorCode code, Map<String, Object> extensions) {
    this.code = code;
    this.extensions = extensions;
  }

  /**
   * Returns a new instance with the given key-value pair added to the extension fields.
   *
   * <p>Common keys: {@code field}, {@code detail}, {@code resourceId}, etc.
   *
   * @param key the field name to include in the JSON output
   * @param value the field value
   * @return a new {@code ErrorDetail} with the extension added
   */
  public ErrorDetail with(String key, Object value) {
    Map<String, Object> updated = new LinkedHashMap<>(extensions);
    updated.put(key, value);
    return new ErrorDetail(code, updated);
  }

  /**
   * @return the machine-readable error code
   */
  public ErrorCode getCode() {
    return code;
  }

  /**
   * Returns the extension fields, serialized as top-level JSON properties.
   *
   * @return map of additional fields, or empty map if none
   */
  @JsonAnyGetter
  public Map<String, Object> getExtensions() {
    return extensions;
  }
}
