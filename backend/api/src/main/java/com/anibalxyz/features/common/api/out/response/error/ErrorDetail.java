package com.anibalxyz.features.common.api.out.response.error;

import com.anibalxyz.features.common.api.out.code.ErrorCode;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a specific error occurrence with machine-readable {@link #code} and contextual
 * metadata through {@link #extensions}.
 *
 * <p>The {@code code} field is stored internally as a {@code String} to enable native Jackson
 * deserialization without custom deserializers. The public API accepts {@link ErrorCode} instances
 * only, ensuring type safety at construction time while remaining transparent to callers.
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

  public ErrorDetail(ErrorCode code) {
    this(code.name(), new LinkedHashMap<>());
  }

  /**
   * Factory method used exclusively by Jackson for deserialization.
   *
   * <p>Not intended for direct use; use {@link #ErrorDetail(ErrorCode)} instead.
   */
  @JsonCreator
  public static ErrorDetail create(
      @JsonProperty("code") String code, @JsonAnySetter Map<String, Object> extensions) {
    return new ErrorDetail(code, extensions != null ? extensions : new LinkedHashMap<>());
  }

  /**
   * Returns the extension fields, serialized as top-level JSON properties.
   *
   * <p>Expose an unmodifiable view of {@code extensions} to prevent external mutation during
   * Jackson deserialization (unknown fields are collected into it via {@code @JsonAnySetter})
   *
   * <p>Returns {@code null} when empty so that {@code @JsonInclude(NON_NULL)} omits it from the
   * serialized output.
   *
   * @return unmodifiable view of the extension fields, or {@code null} if empty
   */
  @JsonAnyGetter
  public Map<String, Object> extensions() {
    if (extensions == null || extensions.isEmpty()) return null;
    return Collections.unmodifiableMap(extensions);
  }

  // TODO: add common 'with' methods, e.g. withTitle()
  public ErrorDetail with(String key, Object value) {
    Map<String, Object> updated = new LinkedHashMap<>(extensions);
    updated.put(key, value);
    return new ErrorDetail(code, updated);
  }
}
