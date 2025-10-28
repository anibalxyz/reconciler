package com.anibalxyz.features.auth;

import static com.anibalxyz.features.Constants.Users.*;
import static com.anibalxyz.features.Helpers.cleanDatabase;
import static com.anibalxyz.features.Helpers.persistUser;
import static org.assertj.core.api.Assertions.assertThat;

import com.anibalxyz.features.Constants;
import com.anibalxyz.features.Constants.Auth.Token;
import com.anibalxyz.features.HttpRequest;
import com.anibalxyz.features.auth.api.AuthRoutes;
import com.anibalxyz.features.auth.api.in.LoginRequest;
import com.anibalxyz.features.auth.api.out.AuthResponse;
import com.anibalxyz.features.auth.application.JwtService;
import com.anibalxyz.features.auth.application.RefreshTokenService;
import com.anibalxyz.features.auth.application.exception.InvalidCredentialsException;
import com.anibalxyz.features.auth.domain.RefreshToken;
import com.anibalxyz.features.auth.domain.RefreshTokenRepository;
import com.anibalxyz.features.auth.infra.JpaRefreshTokenRepository;
import com.anibalxyz.features.common.api.out.ErrorResponse;
import com.anibalxyz.features.users.infra.UserEntity;
import com.anibalxyz.server.Application;
import com.anibalxyz.server.DependencyContainer;
import com.anibalxyz.server.config.modules.runtime.JwtMiddlewareConfig;
import com.anibalxyz.server.config.modules.runtime.SchedulerConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Tests for AuthRoutes")
public class AuthRoutesIntegrationTest {
  private static Application app;
  private static EntityManagerFactory emf;
  private static HttpRequest http;
  private static JwtService jwtService;
  private static RefreshTokenService refreshTokenService;
  RefreshTokenRepository refreshTokenRepository;
  private EntityManager em;

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

    jwtService = new JwtService(Constants.APP_CONFIG.env());
  }

  private static Application createApplication() {
    Consumer<DependencyContainer> customRuntimeConfigs =
        container -> {
          new JwtMiddlewareConfig(container.server(), container.jwtMiddleware()).apply();
          new SchedulerConfig(container.server(), container.refreshTokenService()).apply();
        };
    Consumer<DependencyContainer> customRoutesRegistries =
        container -> {
          new AuthRoutes(container.server(), container.authController()).register();
        };

    return Application.buildApplication(
        Constants.APP_CONFIG, null, customRuntimeConfigs, customRoutesRegistries);
  }

  @AfterAll
  public static void shutdown() {
    app.stop();
  }

  private static boolean isValidToken(String token, Token type) {
    if (token == null || token.isBlank()) return false;
    try {
      switch (type) {
        case ACCESS -> jwtService.validateToken(token);
        case REFRESH -> refreshTokenService.verifyRefreshToken(token);
        default -> throw new IllegalArgumentException("Invalid token type");
      }
      return true;
    } catch (InvalidCredentialsException e) {
      return false;
    }
  }

  private static String getValueFromCookie(String cookie, String key) {
    String result = null;
    for (String cookiePart : cookie.split(";")) {
      if (cookiePart.contains(key)) {
        result = cookiePart.split("=")[1];
      }
    }
    return result;
  }

  @BeforeEach
  public void openEntityManager() {
    em = emf.createEntityManager();
    cleanDatabase(em);
  }

  @BeforeEach
  public void di() {
    refreshTokenRepository = new JpaRefreshTokenRepository(() -> em);
    refreshTokenService =
        new RefreshTokenService(refreshTokenRepository, Constants.APP_CONFIG.env());
  }

  @AfterEach
  public void closeEntityManager() {
    if (em.isOpen()) {
      em.close();
    }
  }

  @Nested
  @DisplayName("Success Scenarios")
  class SuccessScenarios {

    @Test
    @DisplayName(
        "POST /auth/login: given valid credentials, then return 200 OK and JWT with refresh token cookie")
    void POST_auth_login_validCredentials_return200AndJwtWithRefreshTokenCookie() {
      UserEntity user = persistUser(em, VALID_NAME, VALID_EMAIL, VALID_PASSWORD);
      LoginRequest request = new LoginRequest(user.getEmail(), VALID_PASSWORD);

      Response response = http.post("/auth/login", request);
      assertThat(response.code()).isEqualTo(200);

      String cookie = response.header("Set-Cookie");
      assertThat(cookie).isNotNull();

      String refreshToken = getValueFromCookie(cookie, "refreshToken");
      assertThat(refreshToken).isNotNull();
      assertThat(isValidToken(refreshToken, Token.REFRESH)).isTrue();

      AuthResponse body = http.parseBody(response, AuthResponse.class);
      assertThat(body).isNotNull();
      assertThat(isValidToken(body.accessToken(), Token.ACCESS)).isTrue();
    }

    @Test
    @DisplayName(
        "POST /auth/refresh: given valid refresh token, then return 200 OK and new JWT with new refresh token cookie")
    void POST_auth_refresh_validRefreshToken_return200AndNewJwtWithNewRefreshTokenCookie() {
      UserEntity user = persistUser(em, VALID_NAME, VALID_EMAIL, VALID_PASSWORD);
      LoginRequest request = new LoginRequest(user.getEmail(), VALID_PASSWORD);

      String cookie;
      try (Response loginResponse = http.post("/auth/login", request)) {
        assertThat(loginResponse.code()).isEqualTo(200);

        cookie = loginResponse.header("Set-Cookie");
      }

      assertThat(cookie).isNotNull();

      String refreshToken = getValueFromCookie(cookie, "refreshToken");
      assertThat(refreshToken).isNotNull();
      assertThat(isValidToken(refreshToken, Token.REFRESH)).isTrue();

      Map<String, String> headers = Map.of("Cookie", "refreshToken=" + refreshToken);
      Response refreshResponse = http.postWithHeaders("/auth/refresh", headers);
      assertThat(refreshResponse.code()).isEqualTo(200);

      AuthResponse body = http.parseBody(refreshResponse, AuthResponse.class);
      assertThat(body).isNotNull();
      assertThat(isValidToken(body.accessToken(), Token.ACCESS)).isTrue();
    }
  }

  @Nested
  @DisplayName("Failure Scenarios")
  class FailureScenarios {

    @Test
    @DisplayName("POST /auth/login: given invalid password, then return 401 Unauthorized")
    void POST_auth_login_invalidPassword_return401Unauthorized() {
      UserEntity user = persistUser(em, VALID_NAME, VALID_EMAIL, VALID_PASSWORD);
      LoginRequest request =
          new LoginRequest(user.getEmail(), VALID_PASSWORD + "invalid"); // ensure aren't equal

      Response response = http.post("/auth/login", request);
      assertThat(response.code()).isEqualTo(401);

      String cookie = response.header("Set-Cookie");
      assertThat(cookie).isNull();

      ErrorResponse body = http.parseBody(response, ErrorResponse.class);
      assertThat(body.error()).isEqualTo("Invalid credentials");

      assertThat(refreshTokenRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("POST /auth/login: given non-existent email, then return 401 Unauthorized")
    void POST_auth_login_nonExistentEmail_return401Unauthorized() {
      LoginRequest request = new LoginRequest("non-existent@mail.com", VALID_PASSWORD);

      Response response = http.post("/auth/login", request);
      assertThat(response.code()).isEqualTo(401);

      String cookie = response.header("Set-Cookie");
      assertThat(cookie).isNull();

      ErrorResponse body = http.parseBody(response, ErrorResponse.class);
      assertThat(body.error()).isEqualTo("Invalid credentials");

      assertThat(refreshTokenRepository.findAll()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"null,password", "email@example.com,null", "null,null"})
    @DisplayName("POST /auth/login: given missing required fields, then return 400 Bad Request")
    void POST_auth_login_missingRequiredFields_return400BadRequest(String email, String password) {
      String actualEmail = "null".equals(email) ? null : email;
      String actualPassword = "null".equals(password) ? null : password;
      LoginRequest request = new LoginRequest(actualEmail, actualPassword);

      Response response = http.post("/auth/login", request);
      assertThat(response.code()).isEqualTo(400);

      String cookie = response.header("Set-Cookie");
      assertThat(cookie).isNull();

      ErrorResponse body = http.parseBody(response, ErrorResponse.class);
      assertThat(body.error()).isEqualTo("Invalid input provided");

      assertThat(refreshTokenRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("POST /auth/login: given invalid email format, then return 400 Bad Request")
    void POST_auth_login_invalidEmailFormat_return400BadRequest() {
      LoginRequest request = new LoginRequest("invalid-email", VALID_PASSWORD);

      Response response = http.post("/auth/login", request);
      assertThat(response.code()).isEqualTo(400);

      String cookie = response.header("Set-Cookie");
      assertThat(cookie).isNull();

      ErrorResponse body = http.parseBody(response, ErrorResponse.class);
      assertThat(body.error()).isEqualTo("Invalid input provided");

      assertThat(refreshTokenRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName(
        "POST /auth/refresh: given missing refresh token in cookie, then return 401 Unauthorized")
    void POST_auth_refresh_missingRefreshTokenInCookie_return401Unauthorized() {
      Response response = http.postWithHeaders("/auth/refresh", Map.of());
      assertThat(response.code()).isEqualTo(401);

      String cookie = response.header("Set-Cookie");
      assertThat(cookie).isNull();

      ErrorResponse body = http.parseBody(response, ErrorResponse.class);
      assertThat(body.error()).isEqualTo("Unauthorized");

      assertThat(refreshTokenRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName(
        "POST /auth/refresh: given invalid refresh token, then return 401 Invalid credentials")
    void POST_auth_refresh_invalidRefreshToken_return401InvalidCredentials() {
      Map<String, String> headers = Map.of("Cookie", "refreshToken=invalid-token");
      Response response = http.postWithHeaders("/auth/refresh", headers);
      assertThat(response.code()).isEqualTo(401);

      String cookie = response.header("Set-Cookie");
      assertThat(cookie).isNull();

      ErrorResponse body = http.parseBody(response, ErrorResponse.class);
      assertThat(body.error()).isEqualTo("Invalid credentials");

      assertThat(refreshTokenRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName(
        "POST /auth/refresh: given expired refresh token, then return 401 Invalid credentials")
    void POST_auth_refresh_expiredRefreshToken_return401InvalidCredentials() {
      UserEntity user = persistUser(em, VALID_NAME, VALID_EMAIL, VALID_PASSWORD);
      LoginRequest request = new LoginRequest(user.getEmail(), VALID_PASSWORD);

      String cookie;
      try (Response loginResponse = http.post("/auth/login", request)) {
        cookie = loginResponse.header("Set-Cookie");
      }
      assertThat(cookie).isNotNull();
      String refreshToken = getValueFromCookie(cookie, "refreshToken");
      assertThat(refreshToken).isNotNull();

      // Manually expire the token
      em.getTransaction().begin();
      em.createNativeQuery("UPDATE refresh_tokens SET expiry_date = NOW() - INTERVAL '1 second'")
          .executeUpdate();
      em.getTransaction().commit();

      RefreshToken expiredRefreshToken =
          refreshTokenRepository.findByToken(refreshToken).orElseThrow();

      Map<String, String> headers = Map.of("Cookie", "refreshToken=" + refreshToken);
      Response refreshResponse = http.postWithHeaders("/auth/refresh", headers);
      assertThat(refreshResponse.code()).isEqualTo(401);

      String refreshResponseCookie = refreshResponse.header("Set-Cookie");
      assertThat(refreshResponseCookie).isNull();

      ErrorResponse body = http.parseBody(refreshResponse, ErrorResponse.class);
      assertThat(body.error()).isEqualTo("Invalid credentials");

      List<RefreshToken> foundRefreshTokenList = refreshTokenRepository.findAll();
      assertThat(foundRefreshTokenList).hasSize(1);
      assertThat(foundRefreshTokenList.getFirst()).isEqualTo(expiredRefreshToken);
    }

    @Test
    @DisplayName(
        "POST /auth/refresh: given revoked refresh token, then return 401 Invalid credentials")
    void POST_auth_refresh_revokedRefreshToken_return401InvalidCredentials() {
      UserEntity user = persistUser(em, VALID_NAME, VALID_EMAIL, VALID_PASSWORD);
      LoginRequest request = new LoginRequest(user.getEmail(), VALID_PASSWORD);

      String oldRefreshToken;
      try (Response loginResponse = http.post("/auth/login", request)) {
        String cookie = loginResponse.header("Set-Cookie");
        assertThat(cookie).isNotNull();
        oldRefreshToken = getValueFromCookie(cookie, "refreshToken");
        assertThat(oldRefreshToken).isNotNull();
      }

      // Use the refresh token, which will revoke it and issue a new one
      Map<String, String> headers = Map.of("Cookie", "refreshToken=" + oldRefreshToken);
      try (Response refreshResponse = http.postWithHeaders("/auth/refresh", headers)) {
        assertThat(refreshResponse.code()).isEqualTo(200);

        String cookie = refreshResponse.header("Set-Cookie");
        assertThat(cookie).isNotNull();

        String refreshToken = getValueFromCookie(cookie, "refreshToken");
        assertThat(refreshToken).isNotNull();
        assertThat(isValidToken(refreshToken, Token.REFRESH)).isTrue();

        assertThat(refreshTokenRepository.findByToken(oldRefreshToken).orElseThrow().revoked())
            .isTrue(); // assert that effectively was revoked
      }

      // Try to use the old, revoked token again
      Response secondRefreshResponse = http.postWithHeaders("/auth/refresh", headers);
      assertThat(secondRefreshResponse.code()).isEqualTo(401);

      String refreshResponseCookie = secondRefreshResponse.header("Set-Cookie");
      assertThat(refreshResponseCookie).isNull();

      ErrorResponse body = http.parseBody(secondRefreshResponse, ErrorResponse.class);
      assertThat(body.error()).isEqualTo("Invalid credentials");

      int tokensThatShouldBeInDatabase = 2;
      assertThat(refreshTokenRepository.findAll()).hasSize(tokensThatShouldBeInDatabase);
    }
  }
}
