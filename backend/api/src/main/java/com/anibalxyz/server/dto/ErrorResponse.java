package com.anibalxyz.server.dto;

import java.util.Collections;
import java.util.List;

public record ErrorResponse(String error, List<String> details) {
  public ErrorResponse(String error) {
    this(error, Collections.emptyList());
  }
}
