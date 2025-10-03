package com.anibalxyz.users.api;

import com.anibalxyz.users.api.out.UserCreateResponse;
import com.anibalxyz.users.api.out.UserDetailResponse;
import com.anibalxyz.users.domain.User;

public class UserMapper {

  private UserMapper() {} // avoids false lack of coverage

  public static UserDetailResponse toDetailResponse(User user) {
    return new UserDetailResponse(
        user.getId(),
        user.getName(),
        user.getEmail().value(),
        user.getCreatedAt(),
        user.getUpdatedAt());
  }

  public static UserCreateResponse toCreateResponse(User user) {
    return new UserCreateResponse(user.getId(), user.getName(), user.getEmail().value());
  }
}
