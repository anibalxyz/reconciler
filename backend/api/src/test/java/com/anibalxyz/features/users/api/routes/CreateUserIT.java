package com.anibalxyz.features.users.api.routes;

import static com.anibalxyz.shared.Constants.Users.*;
import static com.anibalxyz.shared.Helpers.persistUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.anibalxyz.features.common.api.out.response.error.ErrorResponse;
import com.anibalxyz.features.users.api.in.CreateUserRequest;
import com.anibalxyz.features.users.api.out.CreateUserResponse;
import com.anibalxyz.features.users.domain.Email;
import com.anibalxyz.features.users.domain.User;
import com.anibalxyz.features.users.infra.UserEntity;
import com.anibalxyz.server.api.ErrorResult;
import com.anibalxyz.shared.ResultAsserts;
import java.time.temporal.ChronoUnit;
import okhttp3.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;

@DisplayName("Tests for POST /users")
public class CreateUserIT extends UsersIT {

  @Test
  @DisplayName("given an invalid property, then respond 400 validation error")
  public void invalidProperty_respond400ValidationError() {
    CreateUserRequest requestBody =
        new CreateUserRequest(null, VALID_EMAIL_STRING, VALID_PASSWORD_STRING);

    ErrorResult expectedResult = errorResultFromInvalidName(requestBody.name());

    Response response = http.post("/users", requestBody);
    assertThat(response.code()).isEqualTo(expectedResult.status()).isEqualTo(400);

    ErrorResponse actual = http.parseBody(response, ErrorResponse.class);
    assertThat(actual.instance()).isNotNull();
    assertThat(actual.instance(null)).isEqualTo(expectedResult.response());
    assertThat(userRepository.findByEmail(ResultAsserts.success(Email.of(requestBody.email()))))
        .isEmpty();
  }

  @Test
  @DisplayName("given a missing property, then respond 400 Bad Request")
  public void missingProperty_respond400() {
    CreateUserRequest requestBody =
        new CreateUserRequest(null, VALID_EMAIL_STRING, VALID_PASSWORD_STRING);

    ErrorResult expectedResult = errorResultFromInvalidName(requestBody.name());

    Response response = http.post("/users", requestBody);
    assertThat(response.code()).isEqualTo(expectedResult.status()).isEqualTo(400);

    ErrorResponse actual = http.parseBody(response, ErrorResponse.class);
    assertThat(actual.instance()).isNotNull();
    assertThat(actual.instance(null)).isEqualTo(expectedResult.response());
    assertThat(em.createQuery("SELECT COUNT(u) FROM UserEntity u", Long.class).getSingleResult())
        .isZero();
  }

  @Test
  @DisplayName("given an already taken email, then respond 400 validation error")
  public void alreadyTakenEmail_respond400() {
    String existingEmail = "existing." + VALID_EMAIL_STRING;
    persistUser(em, "existing." + VALID_NAME_STRING, existingEmail);
    CreateUserRequest requestBody =
        new CreateUserRequest(VALID_NAME_STRING, existingEmail, VALID_PASSWORD_STRING);

    ErrorResult expectedResult = errorResultFromAlreadyTakenEmail();

    Response response = http.post("/users", requestBody);
    assertThat(response.code()).isEqualTo(expectedResult.status()).isEqualTo(400);

    ErrorResponse actual = http.parseBody(response, ErrorResponse.class);
    assertThat(actual.instance()).isNotNull();
    assertThat(actual.instance(null)).isEqualTo(expectedResult.response());
    assertThat(userRepository.findAll()).hasSize(1);
  }

  @Test
  @DisplayName("given valid user data, then respond 201 and create the user")
  public void validData_respond201AndCreateUser() {
    CreateUserRequest requestBody =
        new CreateUserRequest("New User", "new.user@mail.com", VALID_PASSWORD_STRING);

    Response response = http.post("/users", requestBody);
    assertThat(response.code()).isEqualTo(201);

    CreateUserResponse responseBody = http.parseBody(response, new TypeReference<>() {});
    User persisted = em.find(UserEntity.class, responseBody.id()).toDomain();

    assertNotNull(persisted);
    assertThat(persisted.passwordMatches(requestBody.password())).isTrue();
    assertThat(persisted.createdAt())
        .isCloseTo(persisted.updatedAt(), within(5, ChronoUnit.SECONDS));
    assertThat(persisted.id().value()).isEqualTo(responseBody.id()).isPositive();
    assertThat(responseBody.name())
        .isEqualTo(requestBody.name())
        .isEqualTo(persisted.name().value());
    assertThat(responseBody.email())
        .isEqualTo(Email.normalize(requestBody.email()))
        .isEqualTo(persisted.email().value());
  }
}
