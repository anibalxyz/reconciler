package com.anibalxyz.features.common.api.out.response;

import com.fasterxml.jackson.annotation.JsonInclude;

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
}
