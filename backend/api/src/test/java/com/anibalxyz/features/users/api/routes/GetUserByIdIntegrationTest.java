package com.anibalxyz.features.users.api.routes;

import static com.anibalxyz.shared.Helpers.createJwtHeader;
import static com.anibalxyz.shared.Helpers.persistUser;
import static org.assertj.core.api.Assertions.assertThat;

import com.anibalxyz.features.users.api.UserMapper;
import com.anibalxyz.features.users.api.out.DetailedUserResponse;
import com.anibalxyz.features.users.domain.User;
import okhttp3.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;

@DisplayName("Tests for GET /users/{id}")
public class GetUserByIdIntegrationTest extends BaseUsersIntegrationTest {

  @Test
  @DisplayName("GET /users/{id}: given an existing user id, then return 200 and the user data")
  public void GET_users_id_existingId_return200AndUser() {
    User user = persistUser(em, "John Doe", "john@mail.com").toDomain();
    DetailedUserResponse expected = UserMapper.toDetailResponse(user);

    Response response = http.get("/users/" + user.id(), createJwtHeader(validJwt));
    assertThat(response.code()).isEqualTo(200);
    assertThat(http.parseBody(response, new TypeReference<DetailedUserResponse>() {}))
        .isEqualTo(expected);
  }
}
