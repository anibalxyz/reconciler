package com.anibalxyz.features.common.api.out.response;

public record CursorPaginationMeta(String method, String next, Integer pageSize)
    implements PaginationMeta {

  public CursorPaginationMeta(String next, Integer pageSize) {
    this("cursor", next, pageSize);
  }
}
