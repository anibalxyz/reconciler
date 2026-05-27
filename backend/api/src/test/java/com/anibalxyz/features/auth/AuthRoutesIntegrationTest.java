package com.anibalxyz.features.auth;

import static com.anibalxyz.features.auth.api.AuthController.REFRESH_TOKEN_COOKIE;
import static com.anibalxyz.shared.Constants.Auth.VALID_REFRESH_TOKEN;
import static com.anibalxyz.shared.Constants.Users.*;
import static com.anibalxyz.shared.Helpers.cleanDatabase;
import static com.anibalxyz.shared.Helpers.persistUser;
import static org.assertj.core.api.Assertions.assertThat;

import com.anibalxyz.features.auth.api.AuthRoutes;
import com.anibalxyz.features.auth.api.in.LoginRequest;
import com.anibalxyz.features.auth.api.out.AuthResponse;
import com.anibalxyz.features.auth.application.AuthService;
import com.anibalxyz.features.auth.application.JwtService;
import com.anibalxyz.features.auth.application.RefreshTokenService;
import com.anibalxyz.features.auth.domain.error.InvalidCredentialsError;
import com.anibalxyz.features.auth.domain.error.InvalidRefreshTokenError;
import com.anibalxyz.features.auth.infra.JpaRefreshTokenRepository;
import com.anibalxyz.features.common.api.out.response.error.ErrorResponse;
import com.anibalxyz.features.common.application.ValidationNotification;
import com.anibalxyz.features.users.domain.Email;
import com.anibalxyz.features.users.domain.User;
import com.anibalxyz.features.users.domain.error.UserDomainError;
import com.anibalxyz.server.Application;
import com.anibalxyz.server.DependencyContainer;
import com.anibalxyz.server.api.ErrorMapper;
import com.anibalxyz.server.api.ErrorResult;
import com.anibalxyz.server.api.InfrastructureErrorMapper;
import com.anibalxyz.server.config.environment.AppEnvironmentSource;
import com.anibalxyz.shared.Constants;
import com.anibalxyz.shared.HttpRequest;
import com.anibalxyz.shared.MutableClock;
import io.javalin.http.UnauthorizedResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.Map;
import java.util.function.Consumer;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.junit.jupiter.api.*;
import tools.jackson.databind.ObjectMapper;

@DisplayName("Tests for AuthRoutes")
public class AuthRoutesIntegrationTest {
  // Tuesday 10:00
  private static final ZonedDateTime FIXED_NOW =
      LocalDateTime.of(2026, 4, 21, 10, 0).atZone(ZoneId.of("America/Montevideo"));
  private static final MutableClock testClock =
      new MutableClock(FIXED_NOW.toInstant(), FIXED_NOW.getZone());
  private static final Instant SATURDAY_MIDDAY =
      FIXED_NOW.with(TemporalAdjusters.next(DayOfWeek.SATURDAY)).with(LocalTime.NOON).toInstant();
  private static final Instant MAINTENANCE_START =
      FIXED_NOW.with(TemporalAdjusters.next(DayOfWeek.MONDAY)).with(LocalTime.of(8, 0)).toInstant();
  private static Application app;
  private static EntityManagerFactory emf;
  private static HttpRequest http;
  private static JwtService jwtService;
  private static AppEnvironmentSource env;
  private static RefreshTokenService refreshTokenService;
  private EntityManager em;

  @BeforeAll
  public static void setup() {
    Constants.init();
    app = createApplication();
    app.start(0);

    String baseUrl = app.javalin().jettyServer().server().getURI().toString() + "api";
    emf = app.persistenceManager().emf();
    ObjectMapper objectMapper =
        new ObjectMapper();

    http = new HttpRequest(objectMapper, new OkHttpClient(), baseUrl);

    jwtService = new JwtService(Constants.APP_CONFIG.env(), testClock);
    env = Constants.APP_CONFIG.env();
  }

  private static Application createApplication() {
    // JwtMiddleware registered internally but unused -> will change once decoupled
    Consumer<DependencyContainer> customRoutesRegistries =
        container -> {
          new AuthRoutes(container.server(), container.authController(), container.jwtMiddleware())
              .register();
        };

    return Application.buildApplication(
        Constants.APP_CONFIG, testClock, null, null, customRoutesRegistries);
  }

  @AfterAll
  public static void shutdown() {
    app.stop();
  }

  private static String getValueFromCookie(String cookie, String key) {
    if (cookie == null) {
      return null;
    }
    for (String cookiePart : cookie.split(";")) {
      String[] parts = cookiePart.trim().split("=");
      if (parts.length > 0 && parts[0].equals(key)) {
        return parts.length > 1 ? parts[1] : "";
      }
    }
    return null;
  }

  private static void validateJwt(String accessToken, Integer id) {
    var jwtValidation = jwtService.validateToken(accessToken);
    assertThat(jwtValidation.isSuccess()).isTrue();

    var jwt = jwtValidation.getValue();
    assertThat(jwt.getSubject()).isEqualTo(id.toString());
    assertThat(jwt.getIssuedAt()).isEqualTo(testClock.instant());
    assertThat(jwt.getIssuer()).isEqualTo(env.JWT_ISSUER());
    assertThat(jwt.getExpiration())
        .isEqualTo(
            Date.from(
                testClock.instant().plusSeconds(env.JWT_ACCESS_EXPIRATION_TIME_MINUTES() * 60)));
  }

  public static void validateRefreshToken(String token, Integer id) {
    assertThat(token).isNotNull();

    var refreshTokenData = refreshTokenService.verifyRefreshToken(token, testClock.instant());
    assertThat(refreshTokenData.isSuccess()).isTrue();
    assertThat(refreshTokenData.getValue().user().id()).isEqualTo(id);
  }

  @BeforeEach
  public void openEntityManager() {
    em = emf.createEntityManager();
    cleanDatabase(em);
  }

  @BeforeEach
  public void di() {
    var refreshTokenRepository = new JpaRefreshTokenRepository(() -> em);
    refreshTokenService = new RefreshTokenService(refreshTokenRepository);
  }

  @BeforeEach
  public void resetClock() {
    testClock.resetTo(FIXED_NOW.toInstant());
  }

  @AfterEach
  public void closeEntityManager() {
    if (em.isOpen()) {
      em.close();
    }
  }

  private LoginResult loginUser(String email, String password) {
    LoginRequest loginRequest = new LoginRequest(email, password);
    Response loginResponse = http.post("/auth/login", loginRequest);
    AuthResponse authResponse = http.parseBody(loginResponse, AuthResponse.class);

    return new LoginResult(
        authResponse.accessToken(),
        getValueFromCookie(loginResponse.header("Set-Cookie"), REFRESH_TOKEN_COOKIE));
  }

  @Test
  @DisplayName("POST /logout: always respond 204 and clear refresh token cookie")
  void logout_always_respond204AndClearCookie() {
    try (Response response = http.post("/auth/logout", "")) {
      assertThat(response.code()).isEqualTo(204);

      String setCookie = response.header("Set-Cookie");
      assertThat(setCookie).isNotNull();
      assertThat(setCookie).contains(REFRESH_TOKEN_COOKIE + "=");
      assertThat(setCookie).contains("Max-Age=0");

      String refreshTokenCookie =
          getValueFromCookie(response.header("Set-Cookie"), REFRESH_TOKEN_COOKIE);
      assertThat(refreshTokenCookie).isNullOrEmpty();
    }
  }

  private record LoginResult(String accessToken, String refreshToken) {}

  @Nested
  @DisplayName("Tests for POST /login")
  class Login {

    @Test
    @DisplayName("given validation failed, then respond with 400 validation error")
    void validationFailed_respond400ValidationError() {
      String invalidEmail = "invalid email";
      LoginRequest loginRequest = new LoginRequest(invalidEmail, VALID_PASSWORD);

      ValidationNotification<UserDomainError> notification = new ValidationNotification<>();
      notification.add("email", Email.validateRaw(invalidEmail).getError());

      ErrorResult expectedResult =
          ErrorMapper.map(new AuthService.AuthenticateUserError.ValidationFailed(notification));

      Response loginResponse = http.post("/auth/login", loginRequest);
      assertThat(loginResponse.code()).isEqualTo(expectedResult.status()).isEqualTo(400);

      ErrorResponse authResponse = http.parseBody(loginResponse, ErrorResponse.class);
      assertThat(authResponse.instance()).isNotNull();
      assertThat(authResponse.instance(null)).isEqualTo(expectedResult.response());
    }

    @Test
    @DisplayName("given outside maintenance window, respond with 503 Unavailable Server")
    void outsideMaintenanceWindow_respond503UnavailableServer() {
      testClock.resetTo(SATURDAY_MIDDAY);
      LoginRequest loginRequest = new LoginRequest(VALID_EMAIL, VALID_PASSWORD);

      ErrorResult expectedResult =
          ErrorMapper.map(
              new AuthService.AuthenticateUserError.MaintenanceWindow(MAINTENANCE_START));

      Response loginResponse = http.post("/auth/login", loginRequest);
      assertThat(loginResponse.code()).isEqualTo(expectedResult.status()).isEqualTo(503);

      ErrorResponse authResponse = http.parseBody(loginResponse, ErrorResponse.class);
      assertThat(authResponse.instance()).isNotNull();
      assertThat(authResponse.instance(null)).isEqualTo(expectedResult.response());
    }

    @Test
    @DisplayName("given invalid credentials, respond with 401 Auth")
    void invalidCredentials_respond401Unauthorized() {
      User user = persistUser(em, VALID_NAME, VALID_EMAIL, VALID_PASSWORD).toDomain();
      LoginRequest loginRequest =
          new LoginRequest("different" + user.email().value(), VALID_PASSWORD);
      ErrorResult expectedResult =
          ErrorMapper.map(
              new AuthService.AuthenticateUserError.InvalidCredentials(
                  new InvalidCredentialsError()));

      Response loginResponse = http.post("/auth/login", loginRequest);
      assertThat(loginResponse.code()).isEqualTo(expectedResult.status()).isEqualTo(401);

      ErrorResponse authResponse = http.parseBody(loginResponse, ErrorResponse.class);
      assertThat(authResponse.instance()).isNotNull();
      assertThat(authResponse.instance(null)).isEqualTo(expectedResult.response());
    }

    @Test
    @DisplayName("given valid credentials, respond 200 with refresh and access tokens")
    void validCredentials_respond200WithTokens() {
      User user = persistUser(em, VALID_NAME, VALID_EMAIL, VALID_PASSWORD).toDomain();
      LoginRequest loginRequest = new LoginRequest(user.email().value(), VALID_PASSWORD);

      Response loginResponse = http.post("/auth/login", loginRequest);
      assertThat(loginResponse.code()).isEqualTo(200);

      AuthResponse authResponse = http.parseBody(loginResponse, AuthResponse.class);
      validateJwt(authResponse.accessToken(), user.id());

      String refreshTokenCookie =
          getValueFromCookie(loginResponse.header("Set-Cookie"), REFRESH_TOKEN_COOKIE);
      validateRefreshToken(refreshTokenCookie, user.id());
    }
  }

  @Nested
  @DisplayName("Tests for POST /refresh")
  class Refresh {

    @Test
    @DisplayName("given missing refreshToken cookie, then respond with 401 Unauthorized")
    void missingCookie_respond401Unauthorized() {
      ErrorResult expectedResult =
          InfrastructureErrorMapper.map(
              new UnauthorizedResponse("Missing refresh token in cookie"));

      Map<String, String> cookie = Map.of("Cookie", REFRESH_TOKEN_COOKIE + "=");

      Response response = http.post("/auth/refresh", "", cookie);
      assertThat(response.code()).isEqualTo(expectedResult.status()).isEqualTo(401);

      String responseCookie =
          getValueFromCookie(response.header("Set-Cookie"), REFRESH_TOKEN_COOKIE);
      assertThat(responseCookie).isNullOrEmpty();

      ErrorResponse errorResponse = http.parseBody(response, ErrorResponse.class);
      assertThat(errorResponse.instance()).isNotNull();
      assertThat(errorResponse.instance(null)).isEqualTo(expectedResult.response());
    }

    @Test
    @DisplayName("given outside maintenance window, respond with 503 Unavailable Server")
    void outsideMaintenanceWindow_respond503UnavailableServer() {
      testClock.resetTo(SATURDAY_MIDDAY);
      ErrorResult expectedResult =
          ErrorMapper.map(
              new AuthService.AuthenticateUserError.MaintenanceWindow(MAINTENANCE_START));

      Map<String, String> cookie =
          Map.of("Cookie", REFRESH_TOKEN_COOKIE + "=" + VALID_REFRESH_TOKEN);
      Response response = http.post("/auth/refresh", "", cookie);
      assertThat(response.code()).isEqualTo(expectedResult.status()).isEqualTo(503);

      String responseCookie =
          getValueFromCookie(response.header("Set-Cookie"), REFRESH_TOKEN_COOKIE);
      assertThat(responseCookie).isNullOrEmpty();

      ErrorResponse errorResponse = http.parseBody(response, ErrorResponse.class);
      assertThat(errorResponse.instance()).isNotNull();
      assertThat(errorResponse.instance(null)).isEqualTo(expectedResult.response());
    }

    @Test
    @DisplayName("given invalid refresh token, then respond with 401 Unauthorized")
    void invalidRefreshToken_respond401Unauthorized() {
      ErrorResult expectedResult =
          ErrorMapper.map(
              new AuthService.RefreshTokensError.InvalidToken(InvalidRefreshTokenError.notFound()));

      // it is called "VALID" referring to the format, but is not a real value
      Map<String, String> cookie =
          Map.of("Cookie", REFRESH_TOKEN_COOKIE + "=" + VALID_REFRESH_TOKEN);

      Response response = http.post("/auth/refresh", "", cookie);
      assertThat(response.code()).isEqualTo(expectedResult.status()).isEqualTo(401);

      String responseCookie =
          getValueFromCookie(response.header("Set-Cookie"), REFRESH_TOKEN_COOKIE);
      assertThat(responseCookie).isNullOrEmpty();

      ErrorResponse errorResponse = http.parseBody(response, ErrorResponse.class);
      assertThat(errorResponse.instance()).isNotNull();
      assertThat(errorResponse.instance(null)).isEqualTo(expectedResult.response());
    }

    @Test
    @DisplayName("given valid refresh token, then respond with 200 with refreshed tokens")
    void validRefreshToken_respond200WithRefreshedTokens() {
      User user = persistUser(em, VALID_NAME, VALID_EMAIL, VALID_PASSWORD).toDomain();
      LoginResult expectedResult = loginUser(user.email().value(), VALID_PASSWORD);

      Map<String, String> cookie =
          Map.of("Cookie", REFRESH_TOKEN_COOKIE + "=" + expectedResult.refreshToken);
      Response response = http.post("/auth/refresh", "", cookie);
      assertThat(response.code()).isEqualTo(200);

      AuthResponse authResponse = http.parseBody(response, AuthResponse.class);
      validateJwt(authResponse.accessToken(), user.id());

      String refreshTokenCookie =
          getValueFromCookie(response.header("Set-Cookie"), REFRESH_TOKEN_COOKIE);
      validateRefreshToken(refreshTokenCookie, user.id());
    }
  }
}
