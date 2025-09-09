package com.anibalxyz.users.api.out;

import java.time.LocalDateTime;

public record UserDetailResponse(
    int id, String name, String email, LocalDateTime createdAt, LocalDateTime updatedAt) {}
