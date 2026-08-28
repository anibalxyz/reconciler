package com.anibalxyz.features.auth.api.routes;

import static com.anibalxyz.features.auth.api.AuthCookieService.REFRESH_TOKEN_COOKIE;
import static com.anibalxyz.shared.Helpers.getValueFromCookie;
import static org.assertj.core.api.Assertions.assertThat;

import com.anibalxyz.shared.IntegrationTest;
import okhttp3.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Tests for POST /logout")
public class LogoutIT extends IntegrationTest {
  @Test
  @DisplayName("POST /logout: always respond 204 and clear refresh token cookie")
  void logout_always_respond204AndClearCookie() {
    try (Response response = http.post("/auth/logout", "")) {
      assertThat(response.code()).isEqualTo(204);

      String setCookie = response.header("Set-Cookie");
      assertThat(setCookie).isNotNull();
      assertThat(setCookie).contains(REFRESH_TOKEN_COOKIE + "=");
      assertThat(setCookie).contains("Expires=Thu, 01 Jan 1970");

      String refreshTokenCookie =
          getValueFromCookie(response.header("Set-Cookie"), REFRESH_TOKEN_COOKIE);
      assertThat(refreshTokenCookie).isNullOrEmpty();
    }
  }
}
