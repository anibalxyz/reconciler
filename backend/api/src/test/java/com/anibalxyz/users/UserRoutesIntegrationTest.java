package com.anibalxyz.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.anibalxyz.persistence.EntityManagerProvider;
import com.anibalxyz.server.Application;
import com.anibalxyz.server.config.AppConfig;
import com.anibalxyz.server.dto.ErrorResponse;
import com.anibalxyz.users.api.UserMapper;
import com.anibalxyz.users.api.in.UserCreateRequest;
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

public class UserRoutesIntegrationTest {
  private static final String VALID_PASSWORD = "V4L|D_Passw0Rd";
  private static Application app;
  private static EntityManagerFactory emf;
  private static OkHttpClient client;
  private static String baseUrl;
  private static ObjectMapper objectMapper;
  private EntityManager em;
  private UserRepository userRepository;

  @BeforeAll
  public static void setup() {
    app = Application.create(AppConfig.loadForTest());
    app.javalin().start(0);

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

  private Response get(String path) throws IOException {
    Request request = new Request.Builder().url(baseUrl + path).get().build();
    return client.newCall(request).execute();
  }

  private Response post(String path, Object body) throws IOException {
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
  }

  private <T> T parseBody(Response response, TypeReference<T> typeRef) throws IOException {
    ResponseBody body = response.body();
    assertNotNull(body);
    return objectMapper.readValue(body.string(), typeRef);
  }

  private UserEntity persistUser(String name, String email) {
    em.getTransaction().begin();

    EntityManagerProvider emp = () -> em;
    User saved =
        new JpaUserRepository(emp)
            .save(new User(name, new Email(email), PasswordHash.generate(VALID_PASSWORD)));

    em.getTransaction().commit();

    UserEntity entity = em.find(UserEntity.class, saved.getId());
    em.refresh(entity);
    return entity;
  }

  @Test
  public void GET_users_returns_list_when_users_exist() throws IOException {
    // ARRANGE
    List<UserEntity> persistedUsers =
        List.of(persistUser("Name", "name@mail.com"), persistUser("Alfredo", "alfredo@mail.com"));
    List<UserDetailResponse> expectedData =
        persistedUsers.stream()
            .map(userEntity -> UserMapper.toDetailResponse(userEntity.toDomain()))
            .toList();

    // ACT
    Response response = get("/users");

    // ASSERT
    assertThat(response.code()).isEqualTo(200);
    List<UserDetailResponse> actualData = parseBody(response, new TypeReference<>() {});
    assertThat(actualData).usingRecursiveFieldByFieldElementComparator().isEqualTo(expectedData);
  }

  @Test
  public void GET_users_returns_empty_list_when_no_users_exist() throws IOException {
    Response response = get("/users");

    assertThat(response.code()).isEqualTo(200);
    List<UserDetailResponse> actualData = parseBody(response, new TypeReference<>() {});
    assertThat(actualData).isEmpty();
  }

  @Test
  public void POST_users_creates_a_new_user() throws IOException {
    // ARRANGE
    UserCreateRequest requestBody =
        new UserCreateRequest("New User", "new.user@mail.com", VALID_PASSWORD);

    // ACT
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
        .isEqualTo(requestBody.email())
        .isEqualTo(persistedUser.getEmail());
  }

  @Test
  public void POST_users_returns_400_when_invalid_property_is_provided() throws IOException {
    UserCreateRequest requestBody = new UserCreateRequest("New User", "new.user@mail.com", "1234");

    Response response = post("/users", requestBody);
    assertThat(response.code()).isEqualTo(400);

    ErrorResponse responseBody = parseBody(response, new TypeReference<>() {});
    assertThat(responseBody.error()).isEqualTo("Invalid argument provided");
    assertThat(responseBody.details()).contains("Password must be at least 8 characters.");

    Optional<User> user = userRepository.findByEmail(new Email(requestBody.email()));
    assertThat(user).isEmpty();
  }

  @Test
  public void POST_users_returns_400_when_there_is_a_missing_property() throws IOException {
    UserCreateRequest requestBody = new UserCreateRequest("New User", "new.user@mail.com", null);

    Response response = post("/users", requestBody);
    assertThat(response.code()).isEqualTo(400);

    ErrorResponse responseBody = parseBody(response, new TypeReference<>() {});
    assertThat(responseBody.error()).isEqualTo("Invalid input provided");
    assertThat(responseBody.details()).contains("Password is required");

    Optional<User> user = userRepository.findByEmail(new Email(requestBody.email()));
    assertThat(user).isEmpty();
  }

  @Test
  public void POST_users_returns_400_when_email_already_exists() throws IOException {
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
  public void POST_users_returns_400_when_unknown_property_is_provided() throws IOException {
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

  // NOTE: this case is not POST /users specific, but for a while it will be here
  @Test
  public void POST_users_returns_400_when_json_is_malformed() throws IOException {
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
}
