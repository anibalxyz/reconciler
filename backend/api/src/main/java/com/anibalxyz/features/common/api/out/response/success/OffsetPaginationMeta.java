package com.anibalxyz.features.common.api.out.response.success;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Objects;

/**
 * Pagination metadata for offset-based strategies.
 *
 * @param method The pagination method, always "offset".
 * @param page The current page number (1-based).
 * @param next The page number for the next page as a string, or {@code null} if this is the last
 *     page.
 * @param pageSize The number of items per page.
 * @param totalCount Total dataset size. Optional to avoid expensive {@code COUNT(*)} queries unless
 *     the page is empty or explicitly required for UI.
 */
public record OffsetPaginationMeta(
    String method,
    Integer page,
    String next,
    Integer pageSize,
    @JsonInclude(JsonInclude.Include.NON_NULL) Integer totalCount)
    implements PaginationMeta {

  public OffsetPaginationMeta(Integer page, String next, Integer pageSize, Integer totalCount) {
    this("offset", page, next, pageSize, totalCount);
  }

  public OffsetPaginationMeta {
    Objects.requireNonNull(method);
    Objects.requireNonNull(page);
    Objects.requireNonNull(pageSize);
  }
}
