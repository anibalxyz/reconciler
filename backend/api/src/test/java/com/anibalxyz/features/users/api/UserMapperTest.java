package com.anibalxyz.features.users.api;

import static com.anibalxyz.shared.Constants.Users.VALID_USER;
import static org.assertj.core.api.Assertions.assertThat;

import com.anibalxyz.features.users.api.out.CreateUserResponse;
import com.anibalxyz.features.users.api.out.DetailedUserResponse;
import com.anibalxyz.features.users.domain.*;
import com.anibalxyz.shared.UnitTest;
import org.junit.jupiter.api.*;

@DisplayName("Tests for UserMapper")
public class UserMapperTest extends UnitTest {
  private static final User user = VALID_USER;

  @Test
  @DisplayName("toDetailResponse: given user, maps all fields correctly and excludes password hash")
  public void toDetailResponse_user_mapsAllFieldsCorrectly() {
    DetailedUserResponse response = UserMapper.toDetailResponse(user);

    assertThat(response.id()).isEqualTo(user.id().value());
    assertThat(response.name()).isEqualTo(user.name().value());
    assertThat(response.email()).isEqualTo(user.email().value());
    assertThat(response.createdAt()).isEqualTo(user.createdAt());
    assertThat(response.updatedAt()).isEqualTo(user.updatedAt());
  }

  @Test
  @DisplayName(
      "toCreateResponse: given user, then maps id, name and email correctly and excludes password hash")
  public void toCreateResponse_user_mapsIdNameAndEmailCorrectly() {
    CreateUserResponse response = UserMapper.toCreateResponse(user);

    assertThat(response.id()).isEqualTo(user.id().value());
    assertThat(response.name()).isEqualTo(user.name().value());
    assertThat(response.email()).isEqualTo(user.email().value());
  }
}
