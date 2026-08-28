package com.anibalxyz.features.auth.application.out;

import com.anibalxyz.features.auth.domain.RawToken;
import java.time.Instant;

public record AuthResult(
    String accessToken, RawToken refreshToken, Instant refreshTokenExpiryDate) {}
