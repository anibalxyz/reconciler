package com.anibalxyz.features.users;

import static com.anibalxyz.features.Constants.Users.VALID_PASSWORD;
import static com.anibalxyz.features.Helper.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.anibalxyz.features.Constants;
import com.anibalxyz.features.HttpRequest;
import com.anibalxyz.features.common.api.out.ErrorResponse;
import com.anibalxyz.features.users.api.UserMapper;
import com.anibalxyz.features.users.api.UserRoutes;
import com.anibalxyz.features.users.api.in.UserCreateRequest;
import com.anibalxyz.features.users.api.in.UserUpdateRequest;
import com.anibalxyz.features.users.api.out.UserCreateResponse;
import com.anibalxyz.features.users.api.out.UserDetailResponse;
import com.anibalxyz.features.users.domain.Email;
import com.anibalxyz.features.users.domain.PasswordHash;
import com.anibalxyz.features.users.domain.User;
import com.anibalxyz.features.users.domain.UserRepository;
import com.anibalxyz.features.users.infra.JpaUserRepository;
import com.anibalxyz.features.users.infra.UserEntity;
import com.anibalxyz.server.Application;
import com.anibalxyz.server.DependencyContainer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

@DisplayName("User Routes Integration Tests")
public class UsersRoutesIntegrationTest {
  private static Application app;
  private static EntityManagerFactory emf;
  private static HttpRequest http;
  private EntityManager em;
  private UserRepository userRepository;

  @BeforeAll
  public static void setup() {
    Constants.init();
    app = createApplication();
    app.start(0);

    String baseUrl = "http://localhost:" + app.javalin().port();
    emf = app.persistenceManager().emf();
    ObjectMapper objectMapper =
        new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    http = new HttpRequest(objectMapper, new OkHttpClient(), baseUrl);
  }

  private static Application createApplication() {
    Consumer<DependencyContainer> customRoutesRegistries =
        container -> {
          new UserRoutes(container.server(), container.userController()).register();
        };

    return Application.buildApplication(Constants.APP_CONFIG, null, null, customRoutesRegistries);
  }

  @AfterAll
  public static void shutdown() {
    app.stop();
  }

  @BeforeEach
  public void openEntityManager() {
    em = emf.createEntityManager();
    userRepository = new JpaUserRepository(() -> em);
    cleanDatabase(em);
  }

  @AfterEach
  public void closeEntityManager() {
    if (em.isOpen()) {
      em.close();
    }
  }

  @Nested
  @DisplayName("Failure Scenarios")
  class FailureScenarios {

    private static Stream<Arguments> provideInvalidPasswordsAndMessages() {
      return Stream.of(
          Arguments.of("short", "Password must be at least 8 characters long"),
          Arguments.of("p".repeat(73), "Password cannot be longer than 72 characters"));
    }

    @Test
    @DisplayName("GET /users/{id}: given a non-existing id, then return 404 Not Found")
    public void GET_users_id_nonExistingId_return404() {
      int nonExistingId = 999;
      ErrorResponse expectedResponse =
          new ErrorResponse(
              "Resource not found", List.of("User with id " + nonExistingId + " not found"));

      Response response = http.get("/users/" + nonExistingId);
      assertThat(response.code()).isEqualTo(404);

      ErrorResponse responseBody = http.parseBody(response, new TypeReference<>() {});
      assertThat(responseBody).isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("GET /users/{id}: given an invalid id format, then return 400 Bad Request")
    public void GET_users_id_invalidIdFormat_return400() {
      String invalidId = "abc";
      ErrorResponse expectedResponse =
          new ErrorResponse("Bad Request", List.of("Invalid ID format. Must be a number."));

      Response response = http.get("/users/" + invalidId);
      assertThat(response.code()).isEqualTo(400);

      ErrorResponse responseBody = http.parseBody(response, new TypeReference<>() {});
      assertThat(responseBody).isEqualTo(expectedResponse);
    }

    @ParameterizedTest
    @MethodSource("provideInvalidPasswordsAndMessages")
    @DisplayName("POST /users: given an invalid password, then return 400 Bad Request")
    public void POST_users_invalidPassword_return400(String password, String message) {
      UserCreateRequest requestBody =
          new UserCreateRequest("New User", "new.user@mail.com", password);

      Response response = http.post("/users", requestBody);
      assertThat(response.code()).isEqualTo(400);

      ErrorResponse responseBody = http.parseBody(response, new TypeReference<>() {});
      assertThat(responseBody.error()).isEqualTo("Invalid input provided");
      assertThat(responseBody.details()).contains(message);

      Optional<User> user = userRepository.findByEmail(new Email(requestBody.email()));
      assertThat(user).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
      "name, null",
      "name, blank",
      "email, null",
      "email, blank",
      "password, null",
      "password, blank"
    })
    @DisplayName("POST /users: given a there is a missing property, then return 400 Bad Request")
    public void POST_users_missingProperty_return400(String missingProp, String value) {
      String invalidValue = value.equals("null") ? null : "";

      String name = missingProp.equals("name") ? invalidValue : "New User";
      String email = missingProp.equals("email") ? invalidValue : "new.user@mail.com";
      String password = missingProp.equals("password") ? invalidValue : VALID_PASSWORD;

      UserCreateRequest requestBody = new UserCreateRequest(name, email, password);

      Response response = http.post("/users", requestBody);
      assertThat(response.code()).isEqualTo(400);

      ErrorResponse responseBody = http.parseBody(response, new TypeReference<>() {});
      assertThat(responseBody.error()).isEqualTo("Invalid input provided");
      assertThat(responseBody.details()).contains(capitalize(missingProp) + " is required");

      Long userCount =
          em.createQuery("SELECT COUNT(u) FROM UserEntity u", Long.class).getSingleResult();
      assertThat(userCount).isZero();
    }

    @Test
    @DisplayName("POST /users: given an existing email, then return 409 Conflict")
    public void POST_users_existingEmail_return409() {
      String existingEmail = "existing.user@mail.com";
      persistUser(em, "Existing User", existingEmail);
      UserCreateRequest requestBody =
          new UserCreateRequest("New User", existingEmail, VALID_PASSWORD);

      Response response = http.post("/users", requestBody);

      assertThat(response.code()).isEqualTo(409);
      ErrorResponse responseBody = http.parseBody(response, new TypeReference<>() {});
      assertThat(responseBody.error()).isEqualTo("Conflict");
      assertThat(responseBody.details()).contains("Email already in use. Please use another");

      assertThat(userRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("POST /users: given there is an unknown property, then return 400 Bad Request")
    public void POST_users_unknownProperty_return400() {
      String unknownProperty = "mail";
      Map<String, String> requestBody = new HashMap<>();
      requestBody.put("name", "New User");
      requestBody.put(unknownProperty, "new.user@mail.com");
      requestBody.put("password", "1234");

      Response response = http.post("/users", requestBody);
      assertThat(response.code()).isEqualTo(400);

      ErrorResponse responseBody = http.parseBody(response, new TypeReference<>() {});
      assertThat(responseBody.error()).isEqualTo("Unknown property in request body");
      assertThat(responseBody.details()).contains("Unknown property: '" + unknownProperty + "'");

      assertThat(userRepository.findAll()).isEmpty();
    }

    // NOTE: this case is not "POST /users"'s specific, but for a while it will be here
    @Test
    @DisplayName("POST /users: given a malformed JSON payload, then return 400 Bad Request")
    public void POST_users_malformedJson_return400() {
      String malformedJson =
"""
{
    "name": "nombe",
    "email":
}
""";

      Response response = http.post("/users", malformedJson);

      assertThat(response.code()).isEqualTo(400);
      ErrorResponse responseBody = http.parseBody(response, new TypeReference<>() {});
      assertThat(responseBody.error()).isEqualTo("Malformed JSON request");
      assertThat(responseBody.details()).contains("Malformed JSON in request body");
    }

    @Test
    @DisplayName("PUT /users/{id}: given an invalid id format, then return 400 Bad Request")
    public void PUT_users_id_invalidIdFormat_return400() {
      String invalidId = "abc";
      UserUpdateRequest request = new UserUpdateRequest("New Name", "new@mail.com", "12345678");
      ErrorResponse expectedResponse =
          new ErrorResponse("Bad Request", List.of("Invalid ID format. Must be a number."));

      Response response = http.put("/users/" + invalidId, request);
      assertThat(response.code()).isEqualTo(400);

      ErrorResponse responseBody = http.parseBody(response, new TypeReference<>() {});
      assertThat(responseBody).isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("PUT /users/{id}: given a non-existing id, then return 404 Not Found")
    public void PUT_users_id_nonExistingId_return404() {
      int nonExistingId = 999;
      UserUpdateRequest request = new UserUpdateRequest("New Name", "new@mail.com", "12345678");
      ErrorResponse expectedResponse =
          new ErrorResponse(
              "Resource not found", List.of("User with id " + nonExistingId + " not found"));

      Response response = http.put("/users/" + nonExistingId, request);
      assertThat(response.code()).isEqualTo(404);

      Optional<User> optionalUser = userRepository.findById(nonExistingId);
      assertThat(optionalUser).isEmpty();

      ErrorResponse responseBody = http.parseBody(response, new TypeReference<>() {});
      assertThat(responseBody).isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("PUT /users/{id}: given an unknown property, then return 400 Bad Request")
    public void PUT_users_id_unknownProperty_return400() {
      UserEntity user = persistUser(em, "John Doe", "john@mail.com");
      int existingId = user.getId();

      String unknownProperty = "mail";
      Map<String, String> requestBody = new HashMap<>();
      requestBody.put("name", "New User");
      requestBody.put(unknownProperty, "new.user@mail.com");
      requestBody.put("password", "1234");

      ErrorResponse expectedResponse =
          new ErrorResponse(
              "Unknown property in request body",
              List.of("Unknown property: '" + unknownProperty + "'"));

      Response response = http.put("/users/" + existingId, requestBody);
      assertThat(response.code()).isEqualTo(400);

      User optionalUser = userRepository.findById(existingId).orElseThrow();
      assertThat(optionalUser).isEqualTo(user.toDomain());

      ErrorResponse responseBody = http.parseBody(response, new TypeReference<>() {});
      assertThat(responseBody).isEqualTo(expectedResponse);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("PUT /users/{id}: given no properties are provided, then return 400 Bad Request")
    public void PUT_users_id_noPropertiesAreProvided_return400(String value) {
      UserEntity user = persistUser(em, "John Doe", "john@mail.com");
      int existingId = user.getId();

      UserUpdateRequest requestBody = new UserUpdateRequest(value, value, value);
      ErrorResponse expectedResponse =
          new ErrorResponse(
              "Invalid input provided",
              List.of("At least one field (name, email, password) must be provided"));

      Response response = http.put("/users/" + existingId, requestBody);
      assertThat(response.code()).isEqualTo(400);

      User optionalUser = userRepository.findById(existingId).orElseThrow();
      assertThat(optionalUser).isEqualTo(user.toDomain());

      ErrorResponse responseBody = http.parseBody(response, new TypeReference<>() {});
      assertThat(responseBody).isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("PUT /users/{id}: given a duplicate email, then return 409 Conflict")
    public void PUT_users_id_duplicateEmail_return409() {
      UserEntity userToUpdate = persistUser(em, "User To Update", "update.me@mail.com");
      UserEntity existingUser = persistUser(em, "Existing User", "existing@mail.com");
      int userToUpdateId = userToUpdate.getId();

      UserUpdateRequest requestBody = new UserUpdateRequest(null, existingUser.getEmail(), null);
      ErrorResponse expectedResponse =
          new ErrorResponse("Conflict", List.of("Email already in use. Please use another"));

      Response response = http.put("/users/" + userToUpdateId, requestBody);
      assertThat(response.code()).isEqualTo(409);

      User userAfterAttempt = userRepository.findById(userToUpdateId).orElseThrow();
      assertThat(userAfterAttempt.getEmail().value()).isEqualTo(userToUpdate.getEmail());

      ErrorResponse responseBody = http.parseBody(response, new TypeReference<>() {});
      assertThat(responseBody).isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("PUT /users/{id}: given an invalid email format, then return 400 Bad Request")
    public void PUT_users_id_invalidEmailFormat_return400() {
      UserEntity originalUser = persistUser(em, "John Doe", "john@mail.com");
      int existingId = originalUser.getId();

      String invalidEmail = "invalid-email";
      UserUpdateRequest requestBody = new UserUpdateRequest("New User", invalidEmail, "12345678");
      ErrorResponse expectedResponse =
          new ErrorResponse(
              "Invalid input provided", List.of("Invalid email format: " + invalidEmail));

      Response response = http.put("/users/" + existingId, requestBody);
      assertThat(response.code()).isEqualTo(400);

      ErrorResponse responseBody = http.parseBody(response, new TypeReference<>() {});
      assertThat(responseBody).isEqualTo(expectedResponse);

      User userAfterAttempt = userRepository.findById(existingId).orElseThrow();
      assertThat(userAfterAttempt).isEqualTo(originalUser.toDomain());
    }

    @ParameterizedTest
    @MethodSource("provideInvalidPasswordsAndMessages")
    @DisplayName("PUT /users/{id}: given an invalid password, then return 400 Bad Request")
    public void PUT_users_id_invalidPassword_return400(String password, String message) {
      UserEntity originalUser = persistUser(em, "Original Name", "original@mail.com");
      int existingId = originalUser.getId();
      UserUpdateRequest requestBody = new UserUpdateRequest(null, null, password);

      Response response = http.put("/users/" + existingId, requestBody);
      assertThat(response.code()).isEqualTo(400);

      ErrorResponse responseBody = http.parseBody(response, new TypeReference<>() {});
      assertThat(responseBody.error()).isEqualTo("Invalid input provided");
      assertThat(responseBody.details()).contains(message);

      User userAfterAttempt = userRepository.findById(existingId).orElseThrow();
      assertThat(userAfterAttempt).isEqualTo(originalUser.toDomain());
    }

    @Test
    @DisplayName("DELETE /users/{id}: given a non-existing id, then return 404")
    public void DELETE_users_id_nonExistingId_return404() {
      int nonExistingId = 999;
      ErrorResponse expectedResponse =
          new ErrorResponse(
              "Resource not found", List.of("User with id " + nonExistingId + " not found"));

      Response response = http.delete("/users/" + nonExistingId);
      assertThat(response.code()).isEqualTo(404);

      ErrorResponse responseBody = http.parseBody(response, new TypeReference<>() {});
      assertThat(responseBody).isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("DELETE /users/{id}: given an invalid id format, then return 400")
    public void DELETE_users_id_invalidIdFormat_return400() {
      String invalidId = "abc";
      ErrorResponse expectedResponse =
          new ErrorResponse("Bad Request", List.of("Invalid ID format. Must be a number."));

      Response response = http.delete("/users/" + invalidId);
      assertThat(response.code()).isEqualTo(400);

      ErrorResponse responseBody = http.parseBody(response, new TypeReference<>() {});
      assertThat(responseBody).isEqualTo(expectedResponse);
    }
  }

  @Nested
  @DisplayName("Success Scenarios")
  class SuccessScenarios {
    @Test
    @DisplayName("GET /users: given users exist, then return 200 and the list of users")
    public void GET_users_usersExist_return200AndListOfUsers() {
      List<UserEntity> persistedUsers =
          List.of(
              persistUser(em, "Name", "name@mail.com"),
              persistUser(em, "Alfredo", "alfredo@mail.com"));
      List<UserDetailResponse> expectedData =
          persistedUsers.stream()
              .map(userEntity -> UserMapper.toDetailResponse(userEntity.toDomain()))
              .toList();

      Response response = http.get("/users");

      assertThat(response.code()).isEqualTo(200);
      List<UserDetailResponse> actualData = http.parseBody(response, new TypeReference<>() {});
      assertThat(actualData).usingRecursiveFieldByFieldElementComparator().isEqualTo(expectedData);
    }

    @Test
    @DisplayName("GET /users: given no users exist, then return 200 and an empty list")
    public void GET_users_noUsersExist_return200AndEmptyList() {
      Response response = http.get("/users");

      assertThat(response.code()).isEqualTo(200);
      List<UserDetailResponse> actualData = http.parseBody(response, new TypeReference<>() {});
      assertThat(actualData).isEmpty();
    }

    @Test
    @DisplayName("GET /users/{id}: given an existing user id, then return 200 and the user data")
    public void GET_users_id_existingId_return200AndUser() {
      UserEntity user = persistUser(em, "John Doe", "john@mail.com");
      UserDetailResponse expectedResponse = UserMapper.toDetailResponse(user.toDomain());
      int existingId = user.getId();

      Response response = http.get("/users/" + existingId);
      assertThat(response.code()).isEqualTo(200);

      UserDetailResponse responseBody = http.parseBody(response, new TypeReference<>() {});
      assertThat(responseBody).isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("POST /users: given valid user data, then return 201 and create the user")
    public void POST_users_validData_return201AndCreateUser() {
      UserCreateRequest requestBody =
          new UserCreateRequest("New User", "new.user@mail.com", VALID_PASSWORD);

      Response response = http.post("/users", requestBody);
      assertThat(response.code()).isEqualTo(201);
      UserCreateResponse responseBody = http.parseBody(response, new TypeReference<>() {});
      UserEntity persistedUser = em.find(UserEntity.class, responseBody.id());

      // Assert persisted user
      String hashedPassword = persistedUser.getPasswordHash();

      assertNotNull(persistedUser);
      assertTrue(PasswordHash.isValidHash(hashedPassword));
      assertTrue(new PasswordHash(hashedPassword).matches(requestBody.password()));
      assertThat(persistedUser.getCreatedAt())
          .isCloseTo(persistedUser.getUpdatedAt(), within(5, ChronoUnit.SECONDS));

      // Assert response body
      assertThat(persistedUser.getId()).isEqualTo(responseBody.id()).isPositive();
      assertThat(responseBody.name())
          .isEqualTo(requestBody.name())
          .isEqualTo(persistedUser.getName());
      assertThat(responseBody.email())
          .isEqualTo(Email.normalize(requestBody.email()))
          .isEqualTo(persistedUser.getEmail());
    }

    @ParameterizedTest
    @ValueSource(strings = {"name", "email", "password"})
    @DisplayName(
        "PUT /users/{id}: given valid id and property, then return 200 and the updated user")
    public void PUT_users_id_validProperty_return200AndUpdatedUser(String updatingProp) {
      UserEntity user = persistUser(em, "John Doe", "john@mail.com");
      PasswordHash prevPasswordHash = new PasswordHash(user.getPasswordHash());
      Instant prevUpdatedAt = user.getUpdatedAt();
      int existingId = user.getId();

      UserUpdateRequest request =
          new UserUpdateRequest(
              updatingProp.equals("name") ? "New User" : null,
              updatingProp.equals("email") ? "new.user@mail.com" : null,
              updatingProp.equals("password") ? ("NEW_" + VALID_PASSWORD) : null);

      Response response = http.put("/users/" + existingId, request);
      assertThat(response.code()).isEqualTo(200);

      UserEntity updatedUser = em.find(UserEntity.class, existingId);
      em.refresh(updatedUser);
      UserDetailResponse responseBody = http.parseBody(response, new TypeReference<>() {});

      switch (updatingProp) {
        case "name":
          assertThat(updatedUser.getName()).isEqualTo(request.name());
          assertThat(responseBody.name()).isEqualTo(request.name());
          break;
        case "email":
          assertThat(updatedUser.getEmail()).isEqualTo(Email.normalize(request.email()));
          assertThat(responseBody.email()).isEqualTo(request.email());
          break;
        case "password":
          PasswordHash updatedPasswordHash = new PasswordHash(updatedUser.getPasswordHash());
          assertTrue(updatedPasswordHash.matches(request.password()));
          assertThat(updatedPasswordHash.value()).isNotEqualTo(prevPasswordHash.value());
          break;
        default:
          break;
      }

      assertThat(updatedUser.getUpdatedAt())
          .isEqualTo(responseBody.updatedAt())
          .isAfter(prevUpdatedAt);
    }

    @Test
    @DisplayName("DELETE /users/{id}: given an existing id, then return 204 and delete the user")
    public void DELETE_users_id_existingId_return204() {
      UserEntity user = persistUser(em, "John Doe", "john@mail.com");
      int existingId = user.getId();

      try (Response response = http.delete("/users/" + existingId)) {
        assertThat(response.code()).isEqualTo(204);
      }

      em.clear();
      Optional<User> optionalUser = userRepository.findById(existingId);
      assertThat(optionalUser).isEmpty();
    }
  }
}
