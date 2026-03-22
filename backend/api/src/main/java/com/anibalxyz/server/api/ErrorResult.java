package com.anibalxyz.server.api;

import com.anibalxyz.features.common.api.out.ErrorResponse;

public record ErrorResult(int status, ErrorResponse response) {}
