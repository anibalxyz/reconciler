package com.anibalxyz.features.auth.api;

import static com.anibalxyz.shared.Helpers.capturedCookie;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.anibalxyz.shared.UnitTest;
import io.javalin.http.Context;
import io.javalin.http.Cookie;
import io.javalin.http.SameSite;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

@DisplayName("Tests for AuthCookieService")
public class AuthCookieServiceTest extends UnitTest {

  private static final Instant NOW = Instant.parse("2025-01-01T12:00:00Z");
  private static final TestEnv TEST_ENV = new TestEnv(true, "example.com", SameSite.STRICT, "/");

  @Mock private Context ctx;
  private AuthCookieService authCookieService;

  @BeforeEach
  void deps() {
    Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    authCookieService = new AuthCookieService(clock, TEST_ENV);
  }

  private record TestEnv(
      Boolean AUTH_COOKIE_SECURE,
      String AUTH_COOKIE_DOMAIN,
      SameSite AUTH_COOKIE_SAMESITE,
      String AUTH_COOKIE_PATH)
      implements AuthCookieService.Env {}

  @Nested
  @DisplayName("Tests for secondsUntilExpiry")
  class SecondsUntilExpiry {

    @Test
    @DisplayName("given a future expiry date, then return remaining seconds")
    void futureExpiryDate_returnRemainingSeconds() {
      Instant future = NOW.plusSeconds(30);

      long result = AuthCookieService.secondsUntilExpiry(future, NOW);

      assertThat(result).isEqualTo(30L);
    }

    @Test
    @DisplayName("given a past expiry date, then return zero")
    void pastExpiryDate_returnZero() {
      Instant past = NOW.minusSeconds(30);

      long result = AuthCookieService.secondsUntilExpiry(past, NOW);

      assertThat(result).isEqualTo(0L);
    }
  }

  @Nested
  @DisplayName("Tests for setRefreshTokenCookie")
  class SetRefreshTokenCookie {

    @Test
    @DisplayName("given valid parameters, then set cookie in context with env configurations")
    void validParameters_setCookieInContextWithEnvConfigs() {
      Instant expiry = NOW.plusSeconds(30);

      authCookieService.setRefreshTokenCookie(ctx, "my-token", expiry);

      Cookie cookie = capturedCookie(ctx);
      assertThat(cookie.getName()).isEqualTo("refreshToken");
      assertThat(cookie.getValue()).isEqualTo("my-token");
      assertThat(cookie.getPath()).isEqualTo("/");
      assertThat(cookie.getSecure()).isTrue();
      assertThat(cookie.getDomain()).isEqualTo("example.com");
      assertThat(cookie.getSameSite()).isEqualTo(SameSite.STRICT);
      assertThat(cookie.getMaxAge()).isEqualTo(30);
    }

    @Test
    @DisplayName("given a past expiry date, then set cookie with zero max age")
    void pastExpiryDate_setCookieWithZeroMaxAge() {
      Instant past = NOW.minusSeconds(30);

      authCookieService.setRefreshTokenCookie(ctx, "any-token", past);

      Cookie cookie = capturedCookie(ctx);
      assertThat(cookie.getValue()).isEqualTo("any-token");
      assertThat(cookie.getMaxAge()).isEqualTo(0);
    }
  }

  @Nested
  @DisplayName("Tests for clearRefreshTokenCookie")
  class ClearRefreshTokenCookie {

    @Test
    @DisplayName("given a context, then clear cookie in context")
    void validContext_clearCookieInContext() {
      authCookieService.clearRefreshTokenCookie(ctx);

      Cookie cookie = capturedCookie(ctx);
      assertThat(cookie.getName()).isEqualTo("refreshToken");
      assertThat(cookie.getValue()).isEmpty();
      assertThat(cookie.getMaxAge()).isEqualTo(0);
    }
  }

  @Nested
  @DisplayName("Tests for getRefreshTokenCookie")
  class GetRefreshTokenCookie {

    @Test
    @DisplayName("given cookie is present in context, then return cookie value")
    void cookiePresentInContext_returnCookieValue() {
      when(ctx.cookie(AuthCookieService.REFRESH_TOKEN_COOKIE)).thenReturn("my-token");

      String result = authCookieService.getRefreshTokenCookie(ctx);

      assertThat(result).isEqualTo("my-token");
    }

    @Test
    @DisplayName("given cookie is not present in context, then return null")
    void cookieNotPresentInContext_returnNull() {
      when(ctx.cookie(AuthCookieService.REFRESH_TOKEN_COOKIE)).thenReturn(null);

      String result = authCookieService.getRefreshTokenCookie(ctx);

      assertThat(result).isNull();
    }
  }
}
