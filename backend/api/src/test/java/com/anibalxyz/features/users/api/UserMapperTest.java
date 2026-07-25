package com.anibalxyz.features.users.api;

import static com.anibalxyz.shared.Constants.Users.VALID_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;

import com.anibalxyz.features.users.api.out.UserCreateResponse;
import com.anibalxyz.features.users.api.out.UserDetailResponse;
import com.anibalxyz.features.users.domain.*;
import com.anibalxyz.shared.Constants;
import com.anibalxyz.shared.ResultAsserts;
import java.time.Instant;
import org.junit.jupiter.api.*;

@DisplayName("Tests for UserMapper")
public class UserMapperTest {
  public static int BCRYPT_LOG_ROUNDS;

  private static User user;

  @BeforeAll
  public static void setup() {
    Constants.init();
    BCRYPT_LOG_ROUNDS = Constants.APP_ENV.BCRYPT_LOG_ROUNDS();
    user =
        new User(
            1,
            ResultAsserts.success(Name.of("John Doe")),
            ResultAsserts.success(Email.of("john@mail.com")),
            ResultAsserts.success(PasswordHash.generate(VALID_PASSWORD, BCRYPT_LOG_ROUNDS)),
            Instant.now(),
            Instant.now());
  }

  @Test
  @DisplayName("toDetailResponse: maps all fields correctly and excludes password hash")
  public void toDetailResponse_mapsAllFieldsCorrectly() {
    UserDetailResponse response = UserMapper.toDetailResponse(user);

    assertThat(response.id()).isEqualTo(user.id());
    assertThat(response.name()).isEqualTo(user.name().value());
    assertThat(response.email()).isEqualTo(user.email().value());
    assertThat(response.createdAt()).isEqualTo(user.createdAt());
    assertThat(response.updatedAt()).isEqualTo(user.updatedAt());
  }

  @Test
  @DisplayName("toCreateResponse: maps id, name and email correctly and excludes password hash")
  public void toCreateResponse_mapsIdNameAndEmailCorrectly() {
    UserCreateResponse response = UserMapper.toCreateResponse(user);

    assertThat(response.id()).isEqualTo(user.id());
    assertThat(response.name()).isEqualTo(user.name().value());
    assertThat(response.email()).isEqualTo(user.email().value());
  }
}
