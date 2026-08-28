package com.anibalxyz.features.users.api.routes;

import static com.anibalxyz.shared.Helpers.createJwtHeader;
import static com.anibalxyz.shared.Helpers.persistUser;
import static org.assertj.core.api.Assertions.assertThat;

import com.anibalxyz.features.users.domain.User;
import okhttp3.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Tests for DELETE /users/{id}")
public class DeleteUserByIdIT extends UsersIT {
  @Test
  @DisplayName("given an existing id, then respond 204 and delete the user")
  public void existingId_respond204() {
    User user = persistUser(em, "John Doe", "john@mail.com").toDomain();
    Integer userId = user.id().value();

    try (Response response = http.delete("/users/" + userId, createJwtHeader(validJwt))) {
      assertThat(response.code()).isEqualTo(204);
    }

    em.clear();
    assertThat(userRepository.findById(userId)).isEmpty();
  }
}
