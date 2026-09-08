package com.anibalxyz.core.api.response.success;

import java.util.Objects;

public record ResponseMeta(PaginationMeta pagination) {
  public ResponseMeta {
    Objects.requireNonNull(pagination);
  }
}
