package com.anibalxyz.users.api.out;

import com.anibalxyz.users.domain.User;

import java.time.LocalDateTime;

public record UserDetailResponse(
    int id, String name, String email, LocalDateTime createdAt, LocalDateTime updatedAt) {

  public static UserDetailResponse fromDomain(User user) {
    return new UserDetailResponse(
        user.getId(),
        user.getName(),
        user.getEmail().value(),
        user.getCreatedAt(),
        user.getUpdatedAt());
  }
}
