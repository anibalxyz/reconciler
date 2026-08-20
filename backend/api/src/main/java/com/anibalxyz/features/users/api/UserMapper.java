package com.anibalxyz.features.users.api;

import com.anibalxyz.features.users.api.out.CreateUserResponse;
import com.anibalxyz.features.users.api.out.DetailedUserResponse;
import com.anibalxyz.features.users.domain.User;

/** Utility class for mapping domain {@link User} objects to API response DTOs. */
public class UserMapper {

  private UserMapper() {}

  public static DetailedUserResponse toDetailResponse(User user) {
    return new DetailedUserResponse(
        user.id().value(),
        user.name().value(),
        user.email().value(),
        user.createdAt(),
        user.updatedAt());
  }

  public static CreateUserResponse toCreateResponse(User user) {
    return new CreateUserResponse(user.id().value(), user.name().value(), user.email().value());
  }
}
