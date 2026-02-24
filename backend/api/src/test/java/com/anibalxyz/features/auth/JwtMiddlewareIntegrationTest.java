package com.anibalxyz.features.auth;

import static com.anibalxyz.features.Constants.Users.*;
import static com.anibalxyz.features.Helpers.cleanDatabase;
import static com.anibalxyz.features.Helpers.persistUser;
import static org.assertj.core.api.Assertions.assertThat;

import com.anibalxyz.features.Constants;
import com.anibalxyz.features.HttpRequest;
import com.anibalxyz.features.auth.api.AuthRoutes;
import com.anibalxyz.features.auth.api.in.LoginRequest;
import com.anibalxyz.features.auth.api.out.AuthResponse;
import com.anibalxyz.features.auth.application.JwtService;
import com.anibalxyz.features.common.api.out.ErrorResponseDeprecated;
import com.anibalxyz.features.users.api.UserMapper;
import com.anibalxyz.features.users.api.UserRoutes;
import com.anibalxyz.features.users.api.out.UserDetailResponse;
import com.anibalxyz.features.users.infra.UserEntity;
import com.anibalxyz.server.Application;
import com.anibalxyz.server.DependencyContainer;
import com.anibalxyz.server.config.modules.runtime.JwtMiddlewareConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Tests for JwtMiddleware")
public class JwtMiddlewareIntegrationTest {
  private static Application app;
  private static EntityManagerFactory emf;
  private static HttpRequest http;
  private JwtService jwtService;
  private EntityManager em;

  @BeforeAll
  public static void setup() {
    Constants.init();
    app = createApplication();
    app.start(0);

    String baseUrl = "http://localhost:" + app.javalin().port() + "/api";
    emf = app.persistenceManager().emf();
    ObjectMapper objectMapper =
        new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    http = new HttpRequest(objectMapper, new OkHttpClient(), baseUrl);
  }

  private static Application createApplication() {
    Consumer<DependencyContainer> customRuntimeConfigs =
        container -> new JwtMiddlewareConfig(container.server(), container.jwtMiddleware()).apply();
    Consumer<DependencyContainer> customRoutesRegistries =
        container -> {
          new UserRoutes(container.server(), container.userController()).register();
          new AuthRoutes(container.server(), container.authController()).register();
        };

    return Application.buildApplication(
        Constants.APP_CONFIG, null, customRuntimeConfigs, customRoutesRegistries);
  }

  @AfterAll
  public static void shutdown() {
    app.stop();
  }

  @BeforeEach
  public void openEntityManager() {
    em = emf.createEntityManager();

    cleanDatabase(em);
  }

  @BeforeEach
  public void di() {
    jwtService = new JwtService(Constants.APP_CONFIG.env());
  }

  @AfterEach
  public void closeEntityManager() {
    if (em.isOpen()) {
      em.close();
    }
  }

  private String loginUser(String email, String password) {
    LoginRequest loginRequest = new LoginRequest(email, password);
    Response loginResponse = http.post("/auth/login", loginRequest);
    AuthResponse authResponse = http.parseBody(loginResponse, new TypeReference<>() {});
    return authResponse.accessToken();
  }

  @Nested
  @DisplayName("Success Scenarios")
  class SuccessScenarios {

    @Test
    @DisplayName("GET /users: given valid JWT, then return 200 OK")
    void GET_users_validJwt_return200Ok() {
      UserEntity userEntity = persistUser(em, VALID_NAME, VALID_EMAIL, VALID_PASSWORD);
      String validJwt = loginUser(userEntity.getEmail(), VALID_PASSWORD);
      List<UserDetailResponse> expectedResponse =
          List.of(UserMapper.toDetailResponse(userEntity.toDomain()));

      Map<String, String> headers = Map.of("Authorization", "Bearer " + validJwt);
      Response response = http.get("/users/", headers);
      assertThat(response.code()).isEqualTo(200);

      List<UserDetailResponse> actualResponseBody =
          http.parseBody(response, new TypeReference<>() {});
      assertThat(actualResponseBody).isNotEmpty();

      assertThat(actualResponseBody).isEqualTo(expectedResponse);
    }
  }

  @Nested
  @DisplayName("Failure Scenarios")
  class FailureScenarios {

    @ParameterizedTest
    @ValueSource(strings = {"missingHeader", "invalidHeader", "missingJwt"})
    @DisplayName("GET /users: given missing JWT, then return 401 Unauthorized")
    void GET_users_missingJwt_return401Unauthorized(String cause) {
      ErrorResponseDeprecated expectedResponseBody =
          new ErrorResponseDeprecated(
              "Unauthorized", List.of("Missing or invalid Authorization header"));

      Map<String, String> headers =
          switch (cause) {
            case "missingHeader" -> Map.of();
            case "invalidHeader" -> Map.of("Authorization", "Beerear ");
            case "missingJwt" -> Map.of("Authorization", "Bearer "); // ctx.header() trims the space
            default -> throw new IllegalStateException("Unexpected value: " + cause);
          };

      Response response = http.get("/users/", headers);
      assertThat(response.code()).isEqualTo(401);

      ErrorResponseDeprecated actualResponseBody =
          http.parseBody(response, ErrorResponseDeprecated.class);
      assertThat(actualResponseBody).isEqualTo(expectedResponseBody);
    }

    @Test
    @DisplayName("GET /users: given invalid JWT, then return 401 Unauthorized")
    void GET_users_invalidJwt_return401Unauthorized() {
      ErrorResponseDeprecated expectedResponseBody =
          new ErrorResponseDeprecated("Invalid credentials", List.of("Invalid JWT token"));

      Map<String, String> headers = Map.of("Authorization", "Bearer " + "invalid-token");
      Response response = http.get("/users/", headers);
      assertThat(response.code()).isEqualTo(401);

      ErrorResponseDeprecated actualResponseBody =
          http.parseBody(response, ErrorResponseDeprecated.class);
      assertThat(actualResponseBody).isEqualTo(expectedResponseBody);
    }

    @Test
    @DisplayName("GET /users: given expired JWT, then return 401 Unauthorized")
    void GET_users_expiredJwt_return401Unauthorized() {
      UserEntity userEntity = persistUser(em, VALID_NAME, VALID_EMAIL, VALID_PASSWORD);
      Instant expiredInstant = Instant.now().minus(1, ChronoUnit.DAYS);
      String expiredJwt = jwtService.generateToken(userEntity.getId(), expiredInstant);

      ErrorResponseDeprecated expectedResponseBody =
          new ErrorResponseDeprecated("Invalid credentials", List.of("JWT has expired"));

      Map<String, String> headers = Map.of("Authorization", "Bearer " + expiredJwt);
      Response response = http.get("/users/", headers);
      assertThat(response.code()).isEqualTo(401);

      ErrorResponseDeprecated actualResponseBody =
          http.parseBody(response, ErrorResponseDeprecated.class);
      assertThat(actualResponseBody).isEqualTo(expectedResponseBody);
    }
  }
}
