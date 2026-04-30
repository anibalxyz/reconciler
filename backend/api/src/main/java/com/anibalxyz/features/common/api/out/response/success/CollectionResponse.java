package com.anibalxyz.features.common.api.out.response.success;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.javalin.openapi.OpenApiName;
import java.util.List;
import java.util.Objects;

/**
 * Standardized response format for resource collections.
 *
 * <p>Wraps resources in a {@link #data} key to allow future extensions (like filtering or summary
 * stats) in the {@link #meta} object without breaking API contracts.
 *
 * @param <T> The type of resources in the collection, must implement {@link SuccessResponse}.
 */
@JsonPropertyOrder({"data", "meta"})
public class CollectionResponse<T extends SuccessResponse> implements SuccessResponse {
  private final List<T> data;
  private final ResponseMeta meta;

  protected CollectionResponse(List<T> data, ResponseMeta meta) {
    this.data = Objects.requireNonNull(data);
    this.meta = Objects.requireNonNull(meta);
  }

  @JsonCreator
  public static <T extends SuccessResponse> CollectionResponse<T> of(
      @JsonProperty("data") List<T> data, @JsonProperty("meta") ResponseMeta meta) {
    List<T> safeData = (data == null) ? List.of() : data;
    return new CollectionResponse<>(safeData, meta);
  }

  /**
   * Generates offset-based metadata for a single-page result set. Useful for small collections that
   * don't yet require true pagination logic.
   *
   * <p>Primarily intended as a temporary bridge for endpoints where full pagination logic has not
   * yet been implemented.
   */
  @JsonCreator
  public static <T extends SuccessResponse> CollectionResponse<T> ofSinglePage(
      @JsonProperty List<T> data) {
    List<T> safeData = (data == null) ? List.of() : data;
    int size = safeData.size();

    var pagination = new OffsetPaginationMeta(1, null, size, size);
    return new CollectionResponse<>(safeData, new ResponseMeta(pagination));
  }

  @JsonGetter("data")
  @OpenApiName("data")
  public List<T> data() {
    return data;
  }

  @JsonGetter("meta")
  @OpenApiName("meta")
  public ResponseMeta meta() {
    return meta;
  }

  @Override
  public int hashCode() {
    return Objects.hash(data, meta);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof CollectionResponse<?> that)) return false;
    return Objects.equals(data, that.data) && Objects.equals(meta, that.meta);
  }

  @Override
  public String toString() {
    return "CollectionResponse[data=%s, meta=%s]".formatted(data, meta);
  }
}
