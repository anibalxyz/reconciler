package com.anibalxyz.features.common.api.out.response.success;

import java.util.Objects;

/**
 * Pagination metadata for cursor-based strategies.
 *
 * @param method The pagination method, always "cursor".
 * @param next An opaque continuation token for the next page, or {@code null} if no more results
 *     exist.
 * @param pageSize The number of items requested per page.
 */
public record CursorPaginationMeta(String method, String next, Integer pageSize)
    implements PaginationMeta {

  public CursorPaginationMeta(String next, Integer pageSize) {
    this("cursor", next, pageSize);
  }

  public CursorPaginationMeta {
    Objects.requireNonNull(method);
    Objects.requireNonNull(pageSize);
  }
}
