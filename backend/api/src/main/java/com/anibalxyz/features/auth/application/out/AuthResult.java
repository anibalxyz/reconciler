package com.anibalxyz.features.auth.application.out;

import com.anibalxyz.features.auth.domain.RefreshToken;

public record AuthResult(String accessToken, RefreshToken refreshToken) {}
