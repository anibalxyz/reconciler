package com.anibalxyz.features.users.api.routes;

import static org.assertj.core.api.Assertions.assertThat;

import com.anibalxyz.features.common.api.out.response.error.ErrorResponse;
import com.anibalxyz.features.users.api.in.UserUpdateRequest;
import com.anibalxyz.features.users.domain.error.UserNotFoundError;
import com.anibalxyz.server.api.ErrorMapper;
import com.anibalxyz.server.api.ErrorResult;
import com.anibalxyz.server.api.InfrastructureErrorMapper;
import io.javalin.http.BadRequestResponse;
import java.util.HashMap;
import java.util.Map;
import okhttp3.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

/*
 * TODO: move to a global test class
 * */
@DisplayName("Tests for contract of /users")
public class UsersContractIntegrationTest extends BaseUsersIntegrationTest {
  @Test
  @DisplayName("ANY /users/{id}: given an invalid id format, then return 400 Bad Request")
  public void ANY_users_id_invalidIdFormat_return400() {
    ErrorResult expectedResult =
        InfrastructureErrorMapper.map(
            new BadRequestResponse("Invalid ID format. Must be a number."));

    Response response = http.get("/users/abc");
    assertThat(400).isEqualTo(response.code()).isEqualTo(expectedResult.status());

    ErrorResponse expected = expectedResult.response();
    ErrorResponse actual = http.parseBody(response, new TypeReference<>() {});
    assertThat(actual.title()).isEqualTo(expected.title());
    assertThat(actual.code()).isEqualTo(expected.code());
    // NOTE: this is fragile as we are assuming BadRequestResponse.message
    //       Once we use custom exception, this will be cleaner
    assertThat(actual.detail()).isEqualTo(expected.detail());
  }

  @ParameterizedTest
  @ValueSource(strings = {"GET", "PUT", "DELETE"})
  @DisplayName("ANY /users/{id}: given a non-existing id, then return 404")
  public void ANY_users_id_nonExistingId_return404(String method) {
    int nonExistingId = 999;
    ErrorResult expectedResult = ErrorMapper.map(UserNotFoundError.byId(nonExistingId));

    Response response =
        switch (method) {
          case "GET" -> http.get("/users/" + nonExistingId);
          case "PUT" ->
              http.put(
                  "/users/" + nonExistingId,
                  new UserUpdateRequest("Name", "email@mail.com", "1234"));
          case "DELETE" -> http.delete("/users/" + nonExistingId);
          default -> throw new IllegalArgumentException("Method not supported");
        };

    assertThat(response.code()).isEqualTo(expectedResult.status()).isEqualTo(404);

    ErrorResponse actualResponse = http.parseBody(response, ErrorResponse.class);
    assertThat(actualResponse.instance()).isNotNull();
    assertThat(actualResponse.instance(null)).isEqualTo(expectedResult.response());
  }

  @ParameterizedTest
  @ValueSource(strings = {"POST", "PUT"})
  @DisplayName("ANY /users: given a malformed JSON payload, then return 400 Bad Request")
  public void ANY_users_malformedJson_return400(String method) {
    String malformedJson =
        """
                        {
                            "name": "name",
                            "email":
                        }
                        """;
    ErrorResult expectedResult = InfrastructureErrorMapper.map(new StreamReadException(""));

    Response response =
        method.equals("POST")
            ? http.post("/users", malformedJson)
            : http.put("/users/1", malformedJson);
    assertThat(response.code()).isEqualTo(400);

    ErrorResponse actual = http.parseBody(response, ErrorResponse.class);
    assertThat(actual.instance()).isNotNull();
    assertThat(actual.instance(null)).isEqualTo(expectedResult.response());
  }

  @ParameterizedTest
  @ValueSource(strings = {"POST", "PUT"})
  @DisplayName("ANY /users: given an unknown property, then return 400 Bad Request")
  public void ANY_users_unknownProperty_return400(String method) {
    String unknownProperty = "mail";
    Map<String, String> requestBody = new HashMap<>();
    requestBody.put("name", "New User");
    requestBody.put(unknownProperty, "new.user@mail.com");
    requestBody.put("password", "1234");

    ErrorResult expectedResult =
        InfrastructureErrorMapper.map(
            new UnrecognizedPropertyException(null, null, null, null, unknownProperty, null));

    Response response =
        method.equals("POST")
            ? http.post("/users", requestBody)
            : http.put("/users/1", requestBody);
    assertThat(response.code()).isEqualTo(expectedResult.status()).isEqualTo(400);

    ErrorResponse actual = http.parseBody(response, ErrorResponse.class);
    ErrorResponse expected = expectedResult.response();
    assertThat(actual.detail()).isEqualTo(expected.detail());
    assertThat(actual.code()).isEqualTo(expected.code());
    assertThat(actual.title()).isEqualTo(expected.title());
    assertThat(actual.instance()).isNotNull();
    assertThat(userRepository.findAll()).isEmpty();
  }
}
