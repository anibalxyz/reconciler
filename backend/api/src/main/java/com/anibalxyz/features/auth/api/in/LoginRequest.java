package com.anibalxyz.features.auth.api.in;

import com.anibalxyz.features.auth.application.in.LoginPayload;

public record LoginRequest(String email, String password) implements LoginPayload {}
