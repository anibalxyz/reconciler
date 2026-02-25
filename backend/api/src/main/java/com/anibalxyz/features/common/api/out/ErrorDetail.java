package com.anibalxyz.features.common.api.out;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a single entry in the {@code errors} array of an {@link ErrorResponse}.
 *
 * <p>Each detail has a required {@code code} (machine-readable identifier) and optional additional
 * fields serialized as top-level JSON properties via {@link #extensions()}.
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
 * <p>Usage example:
 *
 * <pre>{@code
 * new ErrorDetail(ValidationErrorCode.PASSWORD_TOO_SHORT)
 *     .with("field", "#/password")
 *     .with("detail", "Password must be at least 8 characters.");
 * }</pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorDetail(String code, @JsonAnySetter Map<String, Object> extensions) {

  /**
   * Factory method used exclusively by Jackson for deserialization.
   *
   * <p>Accepts {@code code} as a {@code String} to enable native Jackson deserialization without
   * custom deserializers. Unknown JSON fields are collected into {@code extensions} via
   * {@code @JsonAnySetter}.
   *
   * <p>Not intended for direct use — use {@link #ErrorDetail(ErrorCode)} instead.
   *
   * @param code machine-readable error identifier as a plain string
   * @param extensions map of additional fields collected during deserialization
   * @return a new {@code ErrorDetail} instance
   */
  @JsonCreator
  public static ErrorDetail create(
      @JsonProperty("code") String code, @JsonAnySetter Map<String, Object> extensions) {
    return new ErrorDetail(code, extensions != null ? extensions : new LinkedHashMap<>());
  }

  /**
   * Creates a new {@code ErrorDetail} with the given error code and no additional fields.
   *
   * @param code machine-readable error identifier — must be a registered {@link ErrorCode}
   */
  public ErrorDetail(ErrorCode code) {
    this(code.name(), new LinkedHashMap<>());
  }

  /**
   * Returns the extension fields, serialized as top-level JSON properties.
   *
   * <p>{@code @JsonAnyGetter} instructs Jackson to flatten the map entries as root-level properties
   * during serialization (e.g. {@code "field": "#/password"} instead of {@code "extensions":
   * {"field": "#/password"}}).
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
}
