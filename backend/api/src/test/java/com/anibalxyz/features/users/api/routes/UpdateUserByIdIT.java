package com.anibalxyz.features.users.api.routes;

import static com.anibalxyz.shared.Constants.Users.*;
import static com.anibalxyz.shared.Helpers.createJwtHeader;
import static com.anibalxyz.shared.Helpers.persistUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.anibalxyz.features.common.api.out.response.error.ErrorResponse;
import com.anibalxyz.features.users.api.in.CreateUserRequest;
import com.anibalxyz.features.users.api.in.UpdateUserRequest;
import com.anibalxyz.features.users.api.out.DetailedUserResponse;
import com.anibalxyz.features.users.application.UpdateUserById;
import com.anibalxyz.features.users.domain.Email;
import com.anibalxyz.features.users.domain.User;
import com.anibalxyz.features.users.infra.UserEntity;
import com.anibalxyz.server.api.ErrorMapper;
import com.anibalxyz.server.api.ErrorResult;
import java.time.Instant;
import okhttp3.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.core.type.TypeReference;

@DisplayName("Tests for PUT /users/{id}")
public class UpdateUserByIdIT extends UsersIT {

  @Test
  @DisplayName("given no properties provided, then respond 400 Bad Request")
  public void noPropertiesProvided_respond400() {
    User user = persistUser(em, "John Doe", "john@mail.com").toDomain();
    Integer userId = user.id().value();
    UpdateUserRequest requestBody = new UpdateUserRequest(null, null, null);

    ErrorResult expectedResult = ErrorMapper.map(new UpdateUserById.Error.EmptyCommand());

    Response response = http.put("/users/" + userId, requestBody, createJwtHeader(validJwt));
    assertThat(response.code()).isEqualTo(expectedResult.status()).isEqualTo(400);

    assertThat(userRepository.findById(userId).orElseThrow()).isEqualTo(user);
    ErrorResponse actual = http.parseBody(response, ErrorResponse.class);
    assertThat(actual.instance()).isNotNull();
    assertThat(actual.instance(null)).isEqualTo(expectedResult.response());
  }

  @Test
  @DisplayName("given an already taken email, then respond 400 validation error")
  public void alreadyTakenEmail_respond400() {
    User existingUser =
        persistUser(em, "existing." + VALID_NAME_STRING, "existing." + VALID_EMAIL_STRING)
            .toDomain();
    User userToUpdate = persistUser(em, VALID_NAME_STRING, "update.me@mail.com").toDomain();
    UpdateUserRequest requestBody = new UpdateUserRequest(null, existingUser.email().value(), null);

    ErrorResult expectedResult = errorResultFromAlreadyTakenEmail();

    Integer userId = userToUpdate.id().value();
    Response response = http.put("/users/" + userId, requestBody, createJwtHeader(validJwt));
    assertThat(400).isEqualTo(response.code()).isEqualTo(expectedResult.status());
    assertThat(userRepository.findById(userId).orElseThrow().email().value())
        .isEqualTo(userToUpdate.email().value());
    ErrorResponse actual = http.parseBody(response, ErrorResponse.class);
    assertThat(actual.instance()).isNotNull();
    assertThat(actual.instance(null)).isEqualTo(expectedResult.response());
  }

  @Test
  @DisplayName("given an invalid property, then respond 400 validation error")
  public void invalidProperty_respond400ValidationError() {
    User user =
        persistUser(em, VALID_NAME_STRING, VALID_EMAIL_STRING, VALID_PASSWORD_STRING).toDomain();
    Integer userId = user.id().value();
    CreateUserRequest requestBody =
        new CreateUserRequest("  ", VALID_EMAIL_STRING, VALID_PASSWORD_STRING);

    ErrorResult expectedResult = errorResultFromInvalidName(requestBody.name());

    Response response = http.put("/users/" + userId, requestBody, createJwtHeader(validJwt));
    assertThat(response.code()).isEqualTo(expectedResult.status()).isEqualTo(400);

    ErrorResponse actual = http.parseBody(response, ErrorResponse.class);
    assertThat(actual.instance()).isNotNull();
    assertThat(actual.instance(null)).isEqualTo(expectedResult.response());
    assertThat(userRepository.findById(userId).orElseThrow()).isEqualTo(user);
  }

  @ParameterizedTest
  @ValueSource(strings = {"name", "email", "password"})
  @DisplayName("given valid id and property, then respond 200 and the updated user")
  public void validProperty_respond200AndUpdatedUser(String updatingProp) {
    User user = persistUser(em, "John Doe", "john@mail.com").toDomain();
    Integer userId = user.id().value();
    Instant prevUpdatedAt = user.updatedAt();

    UpdateUserRequest request =
        new UpdateUserRequest(
            updatingProp.equals("name") ? "New User" : null,
            updatingProp.equals("email") ? "new.user@mail.com" : null,
            updatingProp.equals("password") ? ("NEW_" + VALID_PASSWORD_STRING) : null);

    Response response = http.put("/users/" + userId, request, createJwtHeader(validJwt));
    assertThat(response.code()).isEqualTo(200);

    UserEntity updatedEntity = em.find(UserEntity.class, userId);
    em.refresh(updatedEntity);
    User updated = updatedEntity.toDomain();

    DetailedUserResponse responseBody = http.parseBody(response, new TypeReference<>() {});

    switch (updatingProp) {
      case "name" -> {
        assertThat(updated.name().value()).isEqualTo(request.name());
        assertThat(responseBody.name()).isEqualTo(request.name());
      }
      case "email" -> {
        assertThat(updated.email().value()).isEqualTo(Email.normalize(request.email()));
        assertThat(responseBody.email()).isEqualTo(request.email());
      }
      case "password" -> assertTrue(updated.passwordMatches(request.password()));
    }

    assertThat(updated.updatedAt()).isEqualTo(responseBody.updatedAt()).isAfter(prevUpdatedAt);
  }
}
