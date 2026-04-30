package com.anibalxyz.features.common.api.out.response.success;

import java.util.Objects;

public record ResponseMeta(PaginationMeta pagination) {
  public ResponseMeta {
    Objects.requireNonNull(pagination);
  }
}
