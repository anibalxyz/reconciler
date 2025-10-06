package com.anibalxyz.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.anibalxyz.persistence.EntityManagerProvider;
import com.anibalxyz.server.Application;
import com.anibalxyz.server.config.AppConfig;
import com.anibalxyz.server.config.EnvVarSet;
import com.anibalxyz.server.dto.ErrorResponse;
import com.anibalxyz.users.api.UserMapper;
import com.anibalxyz.users.api.in.UserCreateRequest;
import com.anibalxyz.users.api.in.UserUpdateRequest;
import com.anibalxyz.users.api.out.UserCreateResponse;
import com.anibalxyz.users.api.out.UserDetailResponse;
import com.anibalxyz.users.domain.Email;
import com.anibalxyz.users.domain.PasswordHash;
import com.anibalxyz.users.domain.User;
import com.anibalxyz.users.domain.UserRepository;
import com.anibalxyz.users.infra.JpaUserRepository;
import com.anibalxyz.users.infra.UserEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

public class UserRoutesIntegrationTest {
  private static final String VALID_PASSWORD = "V4L|D_Passw0Rd";
  private static Application app;
  private static EnvVarSet env;
  private static EntityManagerFactory emf;
  private static OkHttpClient client;
  private static String baseUrl;
  private static ObjectMapper objectMapper;
  private EntityManager em;
  private UserRepository userRepository;

  @BeforeAll
  public static void setup() {
    AppConfig appConfig = AppConfig.loadForTest();
    env = appConfig.env();
    app = Application.create(appConfig);
    app.start(0);

    client = new OkHttpClient();
    baseUrl = "http://localhost:" + app.javalin().port();
    emf = app.persistenceManager().emf();
    objectMapper =
        new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  @AfterAll
  public static void shutdown() {
    app.stop();
  }

  private static String capitalize(String s) {
    if (s == null || s.isEmpty()) return s;
    return s.substring(0, 1).toUpperCase() + s.substring(1);
  }

  @BeforeEach
  public void openEntityManager() {
    em = emf.createEntityManager();
    userRepository = new JpaUserRepository(() -> em);
    cleanDatabase();
  }

  @AfterEach
  public void closeEntityManager() {
    if (em.isOpen()) {
      em.close();
    }
  }

  private void cleanDatabase() {
    em.getTransaction().begin();
    em.createNativeQuery(
            "DO $$ "
                + "DECLARE stmt text; "
                + "BEGIN "
                + "  SELECT 'TRUNCATE TABLE ' || string_agg(quote_ident(tablename), ', ') || ' RESTART IDENTITY CASCADE' "
                + "  INTO stmt "
                + "  FROM pg_tables "
                + "  WHERE schemaname = 'public'; "
                + "  EXECUTE stmt; "
                + "END $$;")
        .executeUpdate();
    em.getTransaction().commit();
  }

  private Response get(String path) {
    Request request = new Request.Builder().url(baseUrl + path).get().build();
    try {
      return client.newCall(request).execute();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private Response post(String path, Object body) {
    try {
      String jsonBody =
          body.getClass().equals(String.class)
              ? (String) body
              : objectMapper.writeValueAsString(body);

      Request request =
          new Request.Builder()
              .url(baseUrl + path)
              .post(okhttp3.RequestBody.create(jsonBody, okhttp3.MediaType.get("application/json")))
              .build();

      return client.newCall(request).execute();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private Response put(String path, Object body) {
    try {
      String jsonBody =
          body.getClass().equals(String.class)
              ? (String) body
              : objectMapper.writeValueAsString(body);

      Request request =
          new Request.Builder()
              .url(baseUrl + path)
              .put(okhttp3.RequestBody.create(jsonBody, okhttp3.MediaType.get("application/json")))
              .build();

      return client.newCall(request).execute();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private Response delete(String path) {
    Request request = new Request.Builder().url(baseUrl + path).delete().build();
    try {
      return client.newCall(request).execute();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private <T> T parseBody(Response response, TypeReference<T> typeRef) {
    try (ResponseBody body = response.body()) {
      assertNotNull(body);
      return objectMapper.readValue(body.string(), typeRef);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private UserEntity persistUser(String name, String email) {
    em.getTransaction().begin();

    EntityManagerProvider emp = () -> em;
    int logRounds = env.BCRYPT_LOG_ROUNDS();
    User saved =
        new JpaUserRepository(emp)
            .save(
                new User(name, new Email(email), PasswordHash.generate(VALID_PASSWORD, logRounds)));

    em.getTransaction().commit();

    UserEntity entity = em.find(UserEntity.class, saved.getId());
    em.refresh(entity);
    return entity;
  }

  @Nested
  @DisplayName("Failure Scenarios")
  class FailureScenarios {

    @Test
    @DisplayName("GET /users/{id}: given a non-existing id, then return 404 Not Found")
    public void GET_users_id_nonExistingId_return404() {
      int nonExistingId = 999;
      ErrorResponse expectedResponse =
          new ErrorResponse(
              "Entity not found", List.of("User with id " + nonExistingId + " not found"));

      Response response = get("/users/" + nonExistingId);
      assertThat(response.code()).isEqualTo(404);

      ErrorResponse responseBody = parseBody(response, new TypeReference<>() {});
      assertThat(responseBody).isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("GET /users/{id}: given an invalid id format, then return 400 Bad Request")
    public void GET_users_id_invalidIdFormat_return400() {
      String invalidId = "abc";
      ErrorResponse expectedResponse =
          new ErrorResponse("Bad Request", List.of("Invalid ID format. Must be a number."));

      Response response = get("/users/" + invalidId);
      assertThat(response.code()).isEqualTo(400);

      ErrorResponse responseBody = parseBody(response, new TypeReference<>() {});
      assertThat(responseBody).isEqualTo(expectedResponse);
    }

    // NOTE: invalid property tested: email
    @Test
    @DisplayName("POST /users: given an invalid password, then return 400 Bad Request")
    public void POST_users_invalidPassword_return400() {
      UserCreateRequest requestBody =
          new UserCreateRequest("New User", "new.user@mail.com", "1234");

      Response response = post("/users", requestBody);
      assertThat(response.code()).isEqualTo(400);

      ErrorResponse responseBody = parseBody(response, new TypeReference<>() {});
      assertThat(responseBody.error()).isEqualTo("Invalid argument provided");
      assertThat(responseBody.details()).contains("Password must be between 8 and 72 characters.");

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

      Response response = post("/users", requestBody);
      assertThat(response.code()).isEqualTo(400);

      ErrorResponse responseBody = parseBody(response, new TypeReference<>() {});
      assertThat(responseBody.error()).isEqualTo("Invalid input provided");
      assertThat(responseBody.details()).contains(capitalize(missingProp) + " is required");

      if (!missingProp.equals("email")) {
        Optional<User> user = userRepository.findByEmail(new Email(requestBody.email()));
        assertThat(user).isEmpty();
      } // TODO: implement 'else' by adding "findByName" method to UserRepository
    }

    @Test
    @DisplayName("POST /users: given an existing email, then return 400 Bad Request")
    public void POST_users_existingEmail_return400() {
      String existingEmail = "existing.user@mail.com";
      persistUser("Existing User", existingEmail);
      UserCreateRequest requestBody =
          new UserCreateRequest("New User", existingEmail, VALID_PASSWORD);

      Response response = post("/users", requestBody);

      assertThat(response.code()).isEqualTo(400);
      ErrorResponse responseBody = parseBody(response, new TypeReference<>() {});
      assertThat(responseBody.error()).isEqualTo("Invalid argument provided");
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

      Response response = post("/users", requestBody);
      assertThat(response.code()).isEqualTo(400);

      ErrorResponse responseBody = parseBody(response, new TypeReference<>() {});
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

      Response response = post("/users", malformedJson);

      assertThat(response.code()).isEqualTo(400);
      ErrorResponse responseBody = parseBody(response, new TypeReference<>() {});
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

      Response response = put("/users/" + invalidId, request);
      assertThat(response.code()).isEqualTo(400);

      ErrorResponse responseBody = parseBody(response, new TypeReference<>() {});
      assertThat(responseBody).isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("PUT /users/{id}: given a non-existing id, then return 404 Not Found")
    public void PUT_users_id_nonExistingId_return404() {
      int nonExistingId = 999;
      UserUpdateRequest request = new UserUpdateRequest("New Name", "new@mail.com", "12345678");
      ErrorResponse expectedResponse =
          new ErrorResponse("Entity not found", List.of("User not found"));

      Response response = put("/users/" + nonExistingId, request);
      assertThat(response.code()).isEqualTo(404);

      Optional<User> optionalUser = userRepository.findById(nonExistingId);
      assertThat(optionalUser).isEmpty();

      ErrorResponse responseBody = parseBody(response, new TypeReference<>() {});
      assertThat(responseBody).isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("PUT /users/{id}: given an unknown property, then return 400 Bad Request")
    public void PUT_users_id_unknownProperty_return400() {
      UserEntity user = persistUser("John Doe", "john@mail.com");
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

      Response response = put("/users/" + existingId, requestBody);
      assertThat(response.code()).isEqualTo(400);

      User optionalUser = userRepository.findById(existingId).orElseThrow();
      assertThat(optionalUser).isEqualTo(user.toDomain());

      ErrorResponse responseBody = parseBody(response, new TypeReference<>() {});
      assertThat(responseBody).isEqualTo(expectedResponse);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("PUT /users/{id}: given no properties are provided, then return 400 Bad Request")
    public void PUT_users_id_noPropertiesAreProvided_return400(String value) {
      UserEntity user = persistUser("John Doe", "john@mail.com");
      int existingId = user.getId();

      UserUpdateRequest requestBody = new UserUpdateRequest(value, value, value);
      ErrorResponse expectedResponse =
          new ErrorResponse(
              "Invalid input provided",
              List.of("At least one field (name, email, password) must be provided"));

      Response response = put("/users/" + existingId, requestBody);
      assertThat(response.code()).isEqualTo(400);

      User optionalUser = userRepository.findById(existingId).orElseThrow();
      assertThat(optionalUser).isEqualTo(user.toDomain());

      ErrorResponse responseBody = parseBody(response, new TypeReference<>() {});
      assertThat(responseBody).isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("PUT /users/{id}: given a duplicate email, then return 400 Bad Request")
    public void PUT_users_id_duplicateEmail_return400() {
      UserEntity userToUpdate = persistUser("User To Update", "update.me@mail.com");
      UserEntity existingUser = persistUser("Existing User", "existing@mail.com");
      int userToUpdateId = userToUpdate.getId();

      UserUpdateRequest requestBody = new UserUpdateRequest(null, existingUser.getEmail(), null);
      ErrorResponse expectedResponse =
          new ErrorResponse(
              "Invalid argument provided", List.of("Email already in use. Please use another"));

      Response response = put("/users/" + userToUpdateId, requestBody);
      assertThat(response.code()).isEqualTo(400);

      User userAfterAttempt = userRepository.findById(userToUpdateId).orElseThrow();
      assertThat(userAfterAttempt.getEmail().value()).isEqualTo(userToUpdate.getEmail());

      ErrorResponse responseBody = parseBody(response, new TypeReference<>() {});
      assertThat(responseBody).isEqualTo(expectedResponse);
    }

    // NOTE: invalid property tested: email
    @Test
    @DisplayName("PUT /users/{id}: given an invalid email format, then return 400 Bad Request")
    public void PUT_users_id_invalidEmailFormat_return400() {
      UserEntity originalUser = persistUser("John Doe", "john@mail.com");
      int existingId = originalUser.getId();

      String invalidEmail = "invalid-email";
      UserUpdateRequest requestBody = new UserUpdateRequest("New User", invalidEmail, "12345678");
      ErrorResponse expectedResponse =
          new ErrorResponse(
              "Invalid argument provided", List.of("Invalid email format: " + invalidEmail));

      Response response = put("/users/" + existingId, requestBody);
      assertThat(response.code()).isEqualTo(400);

      ErrorResponse responseBody = parseBody(response, new TypeReference<>() {});
      assertThat(responseBody).isEqualTo(expectedResponse);

      User userAfterAttempt = userRepository.findById(existingId).orElseThrow();
      assertThat(userAfterAttempt).isEqualTo(originalUser.toDomain());
    }

    @Test
    @DisplayName("DELETE /users/{id}: given a non-existing id, then return 404")
    public void DELETE_users_id_nonExistingId_return404() {
      int nonExistingId = 999;
      ErrorResponse expectedResponse =
          new ErrorResponse(
              "Entity not found", List.of("User with id " + nonExistingId + " not found"));

      Response response = delete("/users/" + nonExistingId);
      assertThat(response.code()).isEqualTo(404);

      ErrorResponse responseBody = parseBody(response, new TypeReference<>() {});
      assertThat(responseBody).isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("DELETE /users/{id}: given an invalid id format, then return 400")
    public void DELETE_users_id_invalidIdFormat_return400() {
      String invalidId = "abc";
      ErrorResponse expectedResponse =
          new ErrorResponse("Bad Request", List.of("Invalid ID format. Must be a number."));

      Response response = delete("/users/" + invalidId);
      assertThat(response.code()).isEqualTo(400);

      ErrorResponse responseBody = parseBody(response, new TypeReference<>() {});
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
          List.of(persistUser("Name", "name@mail.com"), persistUser("Alfredo", "alfredo@mail.com"));
      List<UserDetailResponse> expectedData =
          persistedUsers.stream()
              .map(userEntity -> UserMapper.toDetailResponse(userEntity.toDomain()))
              .toList();

      Response response = get("/users");

      assertThat(response.code()).isEqualTo(200);
      List<UserDetailResponse> actualData = parseBody(response, new TypeReference<>() {});
      assertThat(actualData).usingRecursiveFieldByFieldElementComparator().isEqualTo(expectedData);
    }

    @Test
    @DisplayName("GET /users: given no users exist, then return 200 and an empty list")
    public void GET_users_noUsersExist_return200AndEmptyList() {
      Response response = get("/users");

      assertThat(response.code()).isEqualTo(200);
      List<UserDetailResponse> actualData = parseBody(response, new TypeReference<>() {});
      assertThat(actualData).isEmpty();
    }

    @Test
    @DisplayName("GET /users/{id}: given an existing user id, then return 200 and the user data")
    public void GET_users_id_existingId_return200AndUser() {
      UserEntity user = persistUser("John Doe", "john@mail.com");
      UserDetailResponse expectedResponse = UserMapper.toDetailResponse(user.toDomain());
      int existingId = user.getId();

      Response response = get("/users/" + existingId);
      assertThat(response.code()).isEqualTo(200);

      UserDetailResponse responseBody = parseBody(response, new TypeReference<>() {});
      assertThat(responseBody).isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("POST /users: given valid user data, then return 201 and create the user")
    public void POST_users_validData_return201AndCreateUser() {
      UserCreateRequest requestBody =
          new UserCreateRequest("New User", "new.user@mail.com", VALID_PASSWORD);

      Response response = post("/users", requestBody);
      assertThat(response.code()).isEqualTo(201);
      UserCreateResponse responseBody = parseBody(response, new TypeReference<>() {});
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
      UserEntity user = persistUser("John Doe", "john@mail.com");
      PasswordHash prevPasswordHash = new PasswordHash(user.getPasswordHash());
      LocalDateTime prevUpdatedAt = user.getUpdatedAt();
      int existingId = user.getId();

      UserUpdateRequest request =
          new UserUpdateRequest(
              updatingProp.equals("name") ? "New User" : null,
              updatingProp.equals("email") ? "new.user@mail.com" : null,
              updatingProp.equals("password") ? ("NEW_" + VALID_PASSWORD) : null);

      Response response = put("/users/" + existingId, request);
      assertThat(response.code()).isEqualTo(200);

      UserEntity updatedUser = em.find(UserEntity.class, existingId);
      em.refresh(updatedUser);
      UserDetailResponse responseBody = parseBody(response, new TypeReference<>() {});

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
      UserEntity user = persistUser("John Doe", "john@mail.com");
      int existingId = user.getId();

      try (Response response = delete("/users/" + existingId)) {
        assertThat(response.code()).isEqualTo(204);
      }

      em.clear();
      Optional<User> optionalUser = userRepository.findById(existingId);
      assertThat(optionalUser).isEmpty();
    }
  }
}
