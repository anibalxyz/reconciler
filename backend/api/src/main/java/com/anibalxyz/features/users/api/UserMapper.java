package com.anibalxyz.features.users.api;

import com.anibalxyz.features.users.api.out.UserCreateResponse;
import com.anibalxyz.features.users.api.out.UserDetailResponse;
import com.anibalxyz.features.users.domain.User;

/** Utility class for mapping domain {@link User} objects to API response DTOs. */
public class UserMapper {

  private UserMapper() {}

  public static UserDetailResponse toDetailResponse(User user) {
    return new UserDetailResponse(
        user.id(), user.name().value(), user.email().value(), user.createdAt(), user.updatedAt());
  }

  public static UserCreateResponse toCreateResponse(User user) {
    return new UserCreateResponse(user.id(), user.name().value(), user.email().value());
  }
}
