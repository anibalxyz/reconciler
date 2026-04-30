package com.anibalxyz.features.common.api.out.response.success;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.javalin.openapi.OneOf;

/**
 * Contract for collection pagination metadata.
 *
 * <p>Supports polymorphic strategies (offset or cursor) identified by the {@link #method()} field
 * to guide client-side implementation.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "method")
@JsonSubTypes({
  @JsonSubTypes.Type(value = OffsetPaginationMeta.class, name = "offset"),
  @JsonSubTypes.Type(value = CursorPaginationMeta.class, name = "cursor")
})
@OneOf({OffsetPaginationMeta.class, CursorPaginationMeta.class})
public interface PaginationMeta {

  String method();

  String next();

  Integer pageSize();
}
