package com.anibalxyz.features.auth;

import static com.anibalxyz.shared.Constants.Users.*;
import static com.anibalxyz.shared.Helpers.cleanDatabase;
import static com.anibalxyz.shared.Helpers.persistUser;
import static org.assertj.core.api.Assertions.assertThat;

import com.anibalxyz.features.auth.api.AuthRoutes;
import com.anibalxyz.features.auth.api.JwtMiddleware;
import com.anibalxyz.features.auth.api.in.LoginRequest;
import com.anibalxyz.features.auth.api.out.AuthResponse;
import com.anibalxyz.features.auth.application.JwtService;
import com.anibalxyz.features.common.api.out.response.error.ErrorResponse;
import com.anibalxyz.features.users.api.UserRoutes;
import com.anibalxyz.features.users.domain.User;
import com.anibalxyz.server.Application;
import com.anibalxyz.server.DependencyContainer;
import com.anibalxyz.server.api.ErrorMapper;
import com.anibalxyz.server.api.ErrorResult;
import com.anibalxyz.server.api.InfrastructureErrorMapper;
import com.anibalxyz.shared.Constants;
import com.anibalxyz.shared.HttpRequest;
import io.javalin.Javalin;
import io.javalin.http.UnauthorizedResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.*;
import java.util.Map;
import java.util.function.BiConsumer;
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
import tools.jackson.databind.ObjectMapper;

@DisplayName("Tests for JwtMiddleware")
public class JwtMiddlewareIntegrationTest {
  private static final ZonedDateTime FIXED_NOW =
      LocalDateTime.of(2025, 11, 25, 10, 0).atZone(ZoneId.of("America/Montevideo"));
  private static final Clock testClock = Clock.fixed(FIXED_NOW.toInstant(), FIXED_NOW.getZone());
  private static Application app;
  private static EntityManagerFactory emf;
  private static HttpRequest http;
  private EntityManager em;

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
    BiConsumer<Javalin, DependencyContainer> customRoutesRegistries =
        (server, container) -> {
          new UserRoutes(container.userController()).register(server);
          new AuthRoutes(container.authController(), container.jwtMiddleware()).register(server);
        };

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

    cleanDatabase(em);
  }

  @AfterEach
  public void closeEntityManager() {
    if (em.isOpen()) {
      em.close();
    }
  }

  private String loginUser(String email) {
    LoginRequest loginRequest = new LoginRequest(email, VALID_PASSWORD);
    Response loginResponse = http.post("/auth/login", loginRequest);
    AuthResponse authResponse = http.parseBody(loginResponse, AuthResponse.class);
    return authResponse.accessToken();
  }

  private Map<String, String> authenticationHeaders(String jwt) {
    return Map.of(
        JwtMiddleware.AUTHORIZATION_HEADER, JwtMiddleware.BEARER_PREFIX + (jwt == null ? "" : jwt));
  }

  // NOTE: `ANY_endpoint` tests refer to any *protected* endpoint.
  //       With the current implementation (Apr 13/2026), that means it has a required role != GUEST

  @Nested
  @DisplayName("Success Scenarios")
  class SuccessScenarios {

    @Test
    @DisplayName("ANY /*: given valid JWT, then authorize user")
    void ANY_endpoint_validJwt_authorizeUser() {
      User user = persistUser(em, VALID_NAME, VALID_EMAIL, VALID_PASSWORD).toDomain();
      String validJwt = loginUser(user.email().value());

      Map<String, String> headers = authenticationHeaders(validJwt);
      Response response = http.get("/users", headers);
      assertThat(response.code()).isEqualTo(200);
    }
  }

  @Nested
  @DisplayName("Failure Scenarios")
  class FailureScenarios {

    @ParameterizedTest
    @ValueSource(strings = {"missingHeader", "invalidHeader", "missingJwt"})
    @DisplayName("ANY /*: given missing JWT, then response with 401 Auth")
    void ANY_endpoint_missingJwt_response401Unauthorized(String cause) {
      ErrorResult expectedResult =
          InfrastructureErrorMapper.map(
              new UnauthorizedResponse("Missing or invalid Authorization header"));

      Map<String, String> headers =
          switch (cause) {
            case "missingHeader" -> Map.of();
            case "invalidHeader" ->
                Map.of(JwtMiddleware.AUTHORIZATION_HEADER, "invalid" + JwtMiddleware.BEARER_PREFIX);
            case "missingJwt" ->
                Map.of(JwtMiddleware.AUTHORIZATION_HEADER, JwtMiddleware.BEARER_PREFIX);
            default -> throw new IllegalStateException("Unexpected value: " + cause);
          };

      Response response = http.get("/users/", headers);
      assertThat(response.code()).isEqualTo(expectedResult.status()).isEqualTo(401);

      ErrorResponse actualResponseBody = http.parseBody(response, ErrorResponse.class);
      // NOTE: this is fragile as we are assuming UnauthorizedResponse.message
      //       Once we use custom exception, this will be cleaner
      assertThat(actualResponseBody.instance()).isNotNull();
      assertThat(actualResponseBody.instance(null)).isEqualTo(expectedResult.response());
    }

    @Test
    @DisplayName("GET /users: given invalid JWT, then return 401 Auth")
    void GET_users_invalidJwt_return401Unauthorized() {
      ErrorResult expectedResult = ErrorMapper.map(new JwtService.JwtValidationError.Invalid());

      Map<String, String> headers = authenticationHeaders("invalid-token");
      Response response = http.get("/users/", headers);
      assertThat(response.code()).isEqualTo(expectedResult.status()).isEqualTo(401);

      ErrorResponse actualResponseBody = http.parseBody(response, ErrorResponse.class);
      assertThat(actualResponseBody.instance()).isNotNull();
      assertThat(actualResponseBody.instance(null)).isEqualTo(expectedResult.response());
    }

    @Test
    @DisplayName("GET /users: given expired JWT, then return 401 Auth")
    void GET_users_expiredJwt_return401Unauthorized() {
      User user = persistUser(em, VALID_NAME, VALID_EMAIL, VALID_PASSWORD).toDomain();
      long jwtAccessExpirationTimeMinutes = Constants.APP_ENV.JWT_ACCESS_EXPIRATION_TIME_MINUTES();
      long justExpiredTime = jwtAccessExpirationTimeMinutes + 1;
      Clock clockInThePast = Clock.offset(testClock, Duration.ofMinutes(-justExpiredTime));
      JwtService jwtService = new JwtService(Constants.APP_CONFIG.env(), clockInThePast);
      String expiredJwt = jwtService.generateToken(user.id());

      ErrorResult expectedResult = ErrorMapper.map(new JwtService.JwtValidationError.Expired());

      Map<String, String> headers = authenticationHeaders(expiredJwt);
      Response response = http.get("/users/", headers);
      assertThat(response.code()).isEqualTo(expectedResult.status()).isEqualTo(401);

      ErrorResponse actualResponseBody = http.parseBody(response, ErrorResponse.class);
      assertThat(actualResponseBody.instance()).isNotNull();
      assertThat(actualResponseBody.instance(null)).isEqualTo(expectedResult.response());
    }
  }
}
