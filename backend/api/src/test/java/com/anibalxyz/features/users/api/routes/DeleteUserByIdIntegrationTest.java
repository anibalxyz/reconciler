package com.anibalxyz.features.users.api.routes;

import static com.anibalxyz.shared.Helpers.createJwtHeader;
import static com.anibalxyz.shared.Helpers.persistUser;
import static org.assertj.core.api.Assertions.assertThat;

import com.anibalxyz.features.users.domain.User;
import okhttp3.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Tests for DELETE /users/{id}")
public class DeleteUserByIdIntegrationTest extends BaseUsersIntegrationTest {
  @Test
  @DisplayName("DELETE /users/{id}: given an existing id, then return 204 and delete the user")
  public void DELETE_users_id_existingId_return204() {
    User user = persistUser(em, "John Doe", "john@mail.com").toDomain();

    try (Response response = http.delete("/users/" + user.id(), createJwtHeader(validJwt))) {
      assertThat(response.code()).isEqualTo(204);
    }

    em.clear();
    assertThat(userRepository.findById(user.id())).isEmpty();
  }
}
