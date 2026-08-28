package com.anibalxyz.features.auth.api.routes;

import static com.anibalxyz.shared.Constants.APP_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;

import com.anibalxyz.features.auth.application.*;
import com.anibalxyz.features.auth.infra.JpaRefreshTokenRepository;
import com.anibalxyz.shared.IntegrationTest;
import com.anibalxyz.shared.ResultAsserts;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import org.junit.jupiter.api.*;

public abstract class AuthIT extends IntegrationTest {
  public static final Instant SATURDAY_MIDDAY =
      FIXED_NOW.with(TemporalAdjusters.next(DayOfWeek.SATURDAY)).with(LocalTime.NOON).toInstant();
  static final Instant MAINTENANCE_START =
      FIXED_NOW.with(TemporalAdjusters.next(DayOfWeek.MONDAY)).with(LocalTime.of(8, 0)).toInstant();
  static final JwtService jwtService = new JwtService(APP_CONFIG.env(), testClock);
  static RefreshTokenService refreshTokenService;

  public static void validateJwt(String accessToken, Integer id) {
    var jwt = ResultAsserts.success(jwtService.validateToken(accessToken));
    assertThat(jwt.getSubject()).isEqualTo(id.toString());
    assertThat(jwt.getIssuedAt()).isEqualTo(testClock.instant());
    assertThat(jwt.getIssuer()).isEqualTo(APP_CONFIG.env().JWT_ISSUER());
    assertThat(jwt.getExpiration())
        .isEqualTo(
            Date.from(
                testClock
                    .instant()
                    .plusSeconds(APP_CONFIG.env().JWT_ACCESS_EXPIRATION_TIME_SECONDS())));
  }

  public static void validateRefreshToken(String token, Integer id) {
    assertThat(token).isNotNull();
    var result = refreshTokenService.verifyRefreshToken(token, testClock.instant());
    var refreshToken = ResultAsserts.success(result);
    assertThat(refreshToken.userId().value()).isEqualTo(id);
  }

  @BeforeEach
  public void deps() {
    var refreshTokenRepository = new JpaRefreshTokenRepository(() -> em);
    refreshTokenService = new RefreshTokenService(refreshTokenRepository);
  }
}
