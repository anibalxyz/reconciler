package com.anibalxyz.server.api;

import com.anibalxyz.features.common.api.out.response.error.ErrorResponse;

public record ErrorResult(int status, ErrorResponse response) {}
