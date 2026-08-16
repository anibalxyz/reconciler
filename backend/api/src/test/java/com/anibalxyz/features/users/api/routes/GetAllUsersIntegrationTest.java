package com.anibalxyz.features.users.api.routes;

import static com.anibalxyz.shared.Helpers.createJwtHeader;
import static com.anibalxyz.shared.Helpers.persistUser;
import static org.assertj.core.api.Assertions.assertThat;

import com.anibalxyz.features.common.api.out.response.success.CollectionResponse;
import com.anibalxyz.features.users.api.UserMapper;
import com.anibalxyz.features.users.api.out.DetailedUserResponse;
import com.anibalxyz.features.users.infra.UserEntity;
import java.util.List;
import okhttp3.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;

@DisplayName("Tests for GET /users")
public class GetAllUsersIntegrationTest extends BaseUsersIntegrationTest {

  @Test
  @DisplayName("GET /users: given users exist, then return 200 and the list of users")
  public void GET_users_usersExist_return200AndListOfUsers() {
    List<UserEntity> persisted =
        List.of(
            persistUser(em, "Name", "name@mail.com"),
            persistUser(em, "Alfredo", "alfredo@mail.com"));
    CollectionResponse<DetailedUserResponse> expected =
        CollectionResponse.ofSinglePage(
            persisted.stream().map(u -> UserMapper.toDetailResponse(u.toDomain())).toList());

    Response response = http.get("/users", createJwtHeader(validJwt));
    assertThat(response.code()).isEqualTo(200);
    CollectionResponse<DetailedUserResponse> actual =
        http.parseBody(response, new TypeReference<>() {});
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @DisplayName("GET /users: given no users exist, then return 200 and an empty list")
  public void GET_users_noUsersExist_return200AndEmptyList() {
    Response response = http.get("/users", createJwtHeader(validJwt));
    assertThat(response.code()).isEqualTo(200);
    CollectionResponse<DetailedUserResponse> actual =
        http.parseBody(response, new TypeReference<>() {});
    assertThat(actual.data()).isEmpty();
  }
}
