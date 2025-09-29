package com.anibalxyz.users.api.in;

import com.anibalxyz.users.application.in.UserUpdatePayload;

public record UserCreateRequest(String name, String email, String password)
    implements UserUpdatePayload {}
