package com.anibalxyz.core.api.response.mappers;

import com.anibalxyz.core.api.response.error.ErrorResponse;
import com.anibalxyz.core.primitives.LogEntry;

public record ErrorMapperResult(int status, ErrorResponse response, LogEntry logEntry) {

  public ErrorMapperResult(int status, ErrorResponse response) {
    this(status, response, null);
  }
}
