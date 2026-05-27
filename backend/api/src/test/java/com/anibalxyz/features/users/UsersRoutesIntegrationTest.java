package com.anibalxyz.features.users;

import static com.anibalxyz.shared.Constants.Users.*;
import static com.anibalxyz.shared.Helpers.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.anibalxyz.features.common.api.out.response.error.ErrorResponse;
import com.anibalxyz.features.common.api.out.response.success.CollectionResponse;
import com.anibalxyz.features.common.application.ValidationNotification;
import com.anibalxyz.features.users.api.UserMapper;
import com.anibalxyz.features.users.api.UserRoutes;
import com.anibalxyz.features.users.api.in.UserCreateRequest;
import com.anibalxyz.features.users.api.in.UserUpdateRequest;
import com.anibalxyz.features.users.api.out.UserCreateResponse;
import com.anibalxyz.features.users.api.out.UserDetailResponse;
import com.anibalxyz.features.users.application.UserService;
import com.anibalxyz.features.users.domain.*;
import com.anibalxyz.features.users.domain.error.EmailAlreadyTakenError;
import com.anibalxyz.features.users.domain.error.UserDomainError;
import com.anibalxyz.features.users.domain.error.UserNotFoundError;
import com.anibalxyz.features.users.infra.JpaUserRepository;
import com.anibalxyz.features.users.infra.UserEntity;
import com.anibalxyz.server.Application;
import com.anibalxyz.server.DependencyContainer;
import com.anibalxyz.server.api.ErrorMapper;
import com.anibalxyz.server.api.ErrorResult;
import com.anibalxyz.server.api.InfrastructureErrorMapper;
import com.anibalxyz.shared.Constants;
import com.anibalxyz.shared.HttpRequest;
// TODO: update to 'tools.jackson' once Javalin updated to v7
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import io.javalin.http.BadRequestResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@DisplayName("Tests for UserRoutes")
public class UsersRoutesIntegrationTest {
  private static final ZonedDateTime FIXED_NOW =
      LocalDateTime.of(2025, 11, 25, 10, 0).atZone(ZoneId.of("America/Montevideo"));
  private static final Clock testClock = Clock.fixed(FIXED_NOW.toInstant(), FIXED_NOW.getZone());
  private static final Logger log = LoggerFactory.getLogger(UsersRoutesIntegrationTest.class);
  private static HttpRequest http;
  private static Application app;
  private static EntityManagerFactory emf;
  private EntityManager em;
  private UserRepository userRepository;

  @BeforeAll
  public static void setup() {
    Constants.init();
    app = createApplication();
    app.start(0);

    String baseUrl = app.javalin().jettyServer().server().getURI().toString() + "api";
    emf = app.persistenceManager().emf();
    ObjectMapper objectMapper = new ObjectMapper();

    http = new HttpRequest(objectMapper, new OkHttpClient(), baseUrl);
  }

  private static Application createApplication() {
    Consumer<DependencyContainer> customRoutesRegistries =
        container -> new UserRoutes(container.server(), container.userController()).register();

    return Application.buildApplication(
        Constants.APP_CONFIG, testClock, null, null, customRoutesRegistries);
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
    private static ErrorResult errorResultFromInvalidName(String name) {
      ValidationNotification<UserDomainError> correspondentError = new ValidationNotification<>();
      correspondentError.add("name", Name.validate(name).getError());
      return ErrorMapper.map(correspondentError);
    }

    private static ErrorResult errorResultFromAlreadyTakenEmail() {
      ValidationNotification<UserDomainError> correspondentError = new ValidationNotification<>();
      correspondentError.add("email", new EmailAlreadyTakenError());
      return ErrorMapper.map(correspondentError);
    }

    // --> ANY tests will normally use GET as a lightweight example

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
      ErrorResult expectedResult = InfrastructureErrorMapper.map(new JsonParseException(""));

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

    @Test
    @DisplayName("POST /users: given an invalid property, then return 400 validation error")
    public void POST_users_invalidProperty_return400ValidationError() {
      UserCreateRequest requestBody = new UserCreateRequest(null, VALID_EMAIL, VALID_PASSWORD);

      ErrorResult expectedResult = errorResultFromInvalidName(requestBody.name());

      Response response = http.post("/users", requestBody);
      assertThat(response.code()).isEqualTo(expectedResult.status()).isEqualTo(400);

      ErrorResponse actual = http.parseBody(response, ErrorResponse.class);
      assertThat(actual.instance()).isNotNull();
      assertThat(actual.instance(null)).isEqualTo(expectedResult.response());
      assertThat(userRepository.findByEmail(Email.of(requestBody.email()).getValue())).isEmpty();
    }

    @Test
    @DisplayName("POST /users: given a missing property, then return 400 Bad Request")
    public void POST_users_missingProperty_return400() {
      UserCreateRequest requestBody = new UserCreateRequest(null, VALID_EMAIL, VALID_PASSWORD);

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
    @DisplayName("POST /users: given an already taken email, then return 400 validation error")
    public void POST_users_alreadyTakenEmail_return400() {
      String existingEmail = "existing." + VALID_EMAIL;
      persistUser(em, "existing." + VALID_NAME, existingEmail);
      UserCreateRequest requestBody =
          new UserCreateRequest(VALID_NAME, existingEmail, VALID_PASSWORD);

      ErrorResult expectedResult = errorResultFromAlreadyTakenEmail();

      Response response = http.post("/users", requestBody);
      assertThat(response.code()).isEqualTo(expectedResult.status()).isEqualTo(400);

      ErrorResponse actual = http.parseBody(response, ErrorResponse.class);
      assertThat(actual.instance()).isNotNull();
      assertThat(actual.instance(null)).isEqualTo(expectedResult.response());
      assertThat(userRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("PUT /users/{id}: given no properties provided, then return 400 Bad Request")
    public void PUT_users_id_noPropertiesProvided_return400() {
      User user = persistUser(em, "John Doe", "john@mail.com").toDomain();
      UserUpdateRequest requestBody = new UserUpdateRequest(null, null, null);

      ErrorResult expectedResult =
          ErrorMapper.map(new UserService.UpdateUserByIdError.EmptyCommand());

      Response response = http.put("/users/" + user.id(), requestBody);
      assertThat(response.code()).isEqualTo(expectedResult.status()).isEqualTo(400);

      assertThat(userRepository.findById(user.id()).orElseThrow()).isEqualTo(user);
      ErrorResponse actual = http.parseBody(response, ErrorResponse.class);
      assertThat(actual.instance()).isNotNull();
      assertThat(actual.instance(null)).isEqualTo(expectedResult.response());
    }

    @Test
    @DisplayName("PUT /users/{id}: given an already taken email, then return 400 validation error")
    public void PUT_users_id_alreadyTakenEmail_return400() {
      User existingUser =
          persistUser(em, "existing." + VALID_NAME, "existing." + VALID_EMAIL).toDomain();
      User userToUpdate = persistUser(em, VALID_NAME, "update.me@mail.com").toDomain();
      UserUpdateRequest requestBody =
          new UserUpdateRequest(null, existingUser.email().value(), null);

      ErrorResult expectedResult = errorResultFromAlreadyTakenEmail();

      Response response = http.put("/users/" + userToUpdate.id(), requestBody);
      assertThat(400).isEqualTo(response.code()).isEqualTo(expectedResult.status());
      assertThat(userRepository.findById(userToUpdate.id()).orElseThrow().email().value())
          .isEqualTo(userToUpdate.email().value());
      ErrorResponse actual = http.parseBody(response, ErrorResponse.class);
      assertThat(actual.instance()).isNotNull();
      assertThat(actual.instance(null)).isEqualTo(expectedResult.response());
    }

    @Test
    @DisplayName("PUT /users/{id}: given an invalid property, then return 400 validation error")
    public void PUT_users_id_invalidProperty_return400ValidationError() {
      User user = persistUser(em, VALID_NAME, VALID_EMAIL, VALID_PASSWORD).toDomain();
      UserCreateRequest requestBody = new UserCreateRequest("  ", VALID_EMAIL, VALID_PASSWORD);

      ErrorResult expectedResult = errorResultFromInvalidName(requestBody.name());

      Response response = http.put("/users/" + user.id(), requestBody);
      assertThat(response.code()).isEqualTo(expectedResult.status()).isEqualTo(400);

      ErrorResponse actual = http.parseBody(response, ErrorResponse.class);
      assertThat(actual.instance()).isNotNull();
      assertThat(actual.instance(null)).isEqualTo(expectedResult.response());
      assertThat(userRepository.findById(user.id()).orElseThrow()).isEqualTo(user);
    }
  }

  @Nested
  @DisplayName("Success Scenarios")
  class SuccessScenarios {

    @Test
    @DisplayName("GET /users: given users exist, then return 200 and the list of users")
    public void GET_users_usersExist_return200AndListOfUsers() {
      List<UserEntity> persisted =
          List.of(
              persistUser(em, "Name", "name@mail.com"),
              persistUser(em, "Alfredo", "alfredo@mail.com"));
      CollectionResponse<UserDetailResponse> expected =
          CollectionResponse.ofSinglePage(
              persisted.stream().map(u -> UserMapper.toDetailResponse(u.toDomain())).toList());

      Response response = http.get("/users");
      assertThat(response.code()).isEqualTo(200);
      CollectionResponse<UserDetailResponse> actual =
          http.parseBody(response, new TypeReference<>() {});
      assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("GET /users: given no users exist, then return 200 and an empty list")
    public void GET_users_noUsersExist_return200AndEmptyList() {
      Response response = http.get("/users");
      assertThat(response.code()).isEqualTo(200);
      CollectionResponse<UserDetailResponse> actual =
          http.parseBody(response, new TypeReference<>() {});
      assertThat(actual.data()).isEmpty();
    }

    @Test
    @DisplayName("GET /users/{id}: given an existing user id, then return 200 and the user data")
    public void GET_users_id_existingId_return200AndUser() {
      User user = persistUser(em, "John Doe", "john@mail.com").toDomain();
      UserDetailResponse expected = UserMapper.toDetailResponse(user);

      Response response = http.get("/users/" + user.id());
      assertThat(response.code()).isEqualTo(200);
      assertThat(http.parseBody(response, new TypeReference<UserDetailResponse>() {}))
          .isEqualTo(expected);
    }

    @Test
    @DisplayName("POST /users: given valid user data, then return 201 and create the user")
    public void POST_users_validData_return201AndCreateUser() {
      UserCreateRequest requestBody =
          new UserCreateRequest("New User", "new.user@mail.com", VALID_PASSWORD);

      Response response = http.post("/users", requestBody);
      assertThat(response.code()).isEqualTo(201);

      UserCreateResponse responseBody = http.parseBody(response, new TypeReference<>() {});
      User persisted = em.find(UserEntity.class, responseBody.id()).toDomain();

      assertNotNull(persisted);
      assertTrue(
          PasswordHash.of(persisted.passwordHash().value())
              .getValue()
              .matches(requestBody.password()));
      assertThat(persisted.createdAt())
          .isCloseTo(persisted.updatedAt(), within(5, ChronoUnit.SECONDS));
      assertThat(persisted.id()).isEqualTo(responseBody.id()).isPositive();
      assertThat(responseBody.name())
          .isEqualTo(requestBody.name())
          .isEqualTo(persisted.name().value());
      assertThat(responseBody.email())
          .isEqualTo(Email.normalize(requestBody.email()))
          .isEqualTo(persisted.email().value());
    }

    @ParameterizedTest
    @ValueSource(strings = {"name", "email", "password"})
    @DisplayName(
        "PUT /users/{id}: given valid id and property, then return 200 and the updated user")
    public void PUT_users_id_validProperty_return200AndUpdatedUser(String updatingProp) {
      User user = persistUser(em, "John Doe", "john@mail.com").toDomain();
      PasswordHash prevPasswordHash = PasswordHash.of(user.passwordHash().value()).getValue();
      Instant prevUpdatedAt = user.updatedAt();

      UserUpdateRequest request =
          new UserUpdateRequest(
              updatingProp.equals("name") ? "New User" : null,
              updatingProp.equals("email") ? "new.user@mail.com" : null,
              updatingProp.equals("password") ? ("NEW_" + VALID_PASSWORD) : null);

      Response response = http.put("/users/" + user.id(), request);
      assertThat(response.code()).isEqualTo(200);

      UserEntity updatedEntity = em.find(UserEntity.class, user.id());
      em.refresh(updatedEntity);
      User updated = updatedEntity.toDomain();

      UserDetailResponse responseBody = http.parseBody(response, new TypeReference<>() {});

      switch (updatingProp) {
        case "name" -> {
          assertThat(updated.name().value()).isEqualTo(request.name());
          assertThat(responseBody.name()).isEqualTo(request.name());
        }
        case "email" -> {
          assertThat(updated.email().value()).isEqualTo(Email.normalize(request.email()));
          assertThat(responseBody.email()).isEqualTo(request.email());
        }
        case "password" -> {
          PasswordHash updatedHash = PasswordHash.of(updated.passwordHash().value()).getValue();
          assertTrue(updatedHash.matches(request.password()));
          assertThat(updatedHash.value()).isNotEqualTo(prevPasswordHash.value());
        }
      }

      assertThat(updated.updatedAt()).isEqualTo(responseBody.updatedAt()).isAfter(prevUpdatedAt);
    }

    @Test
    @DisplayName("DELETE /users/{id}: given an existing id, then return 204 and delete the user")
    public void DELETE_users_id_existingId_return204() {
      User user = persistUser(em, "John Doe", "john@mail.com").toDomain();

      try (Response response = http.delete("/users/" + user.id())) {
        assertThat(response.code()).isEqualTo(204);
      }

      em.clear();
      assertThat(userRepository.findById(user.id())).isEmpty();
    }
  }
}
