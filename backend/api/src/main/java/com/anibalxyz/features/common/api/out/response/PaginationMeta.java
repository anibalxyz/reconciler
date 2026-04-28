package com.anibalxyz.features.common.api.out.response;

public interface PaginationMeta {
  String method();

  String next();

  Integer pageSize();
}
