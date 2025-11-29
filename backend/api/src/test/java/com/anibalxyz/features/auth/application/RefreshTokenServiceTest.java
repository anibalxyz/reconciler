package com.anibalxyz.features.auth.application;

import static com.anibalxyz.features.Constants.Auth.VALID_REFRESH_TOKEN;
import static com.anibalxyz.features.Constants.Environment.JWT_REFRESH_EXPIRATION_TIME_DAYS;
import static com.anibalxyz.features.Constants.Users.VALID_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import com.anibalxyz.features.Constants;
import com.anibalxyz.features.auth.application.env.RefreshTokenEnvironment;
import com.anibalxyz.features.auth.application.exception.InvalidCredentialsException;
import com.anibalxyz.features.auth.domain.RefreshToken;
import com.anibalxyz.features.auth.domain.RefreshTokenRepository;
import jakarta.persistence.PersistenceException;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests for RefreshTokenService")
public class RefreshTokenServiceTest {
  public static Supplier<ZonedDateTime> defaultClock =
      () -> ZonedDateTime.now(ZoneId.of("America/Montevideo"));
  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private RefreshTokenEnvironment env;
  @Mock private Supplier<ZonedDateTime> clock;

  @InjectMocks private RefreshTokenService refreshTokenService;

  @BeforeAll
  public static void init() {
    Constants.init();
  }

  @Nested
  @DisplayName("Success Scenarios")
  class SuccessfulScenarios {
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("createRefreshToken: given valid user, then return refresh token")
    public void createRefreshToken_validUser_returnRefreshToken(boolean useWindow) {
      RefreshToken expectedRefreshToken =
          new RefreshToken(
              1L, VALID_REFRESH_TOKEN, VALID_USER, Instant.now().plusSeconds(2), false);

      when(clock.get()).thenReturn(defaultClock.get());
      when(env.JWT_REFRESH_EXPIRATION_TIME_DAYS()).thenReturn(JWT_REFRESH_EXPIRATION_TIME_DAYS);
      when(env.AUTH_ENABLE_TIME_WINDOW()).thenReturn(useWindow);
      when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(expectedRefreshToken);

      RefreshToken actualRefreshToken = refreshTokenService.createRefreshToken(VALID_USER);
      assertThat(actualRefreshToken).isEqualTo(expectedRefreshToken);
    }

    @Test
    @DisplayName(
        "createRefreshToken: given time window is enabled and expiry is after next Friday, then cap expiry to next Friday 20hs")
    void createRefreshToken_windowEnabledAndExpiryAfterNextFriday_capExpiryToNextFriday() {
      // Set the clock to a Thursday 10 hs. With a 7-day expiration,
      // the token would normally (without being capped) expire next Thursday 10 hs.
      ZonedDateTime thursday =
          LocalDateTime.of(2025, 11, 27, 10, 0).atZone(ZoneId.of("America/Montevideo"));
      when(clock.get()).thenReturn(thursday);

      // Cap the expiry to the upcoming Friday at 20:00 (relative to selected Thursday).
      Instant expectedExpiry =
          thursday
              .with(TemporalAdjusters.next(DayOfWeek.FRIDAY))
              .withHour(20)
              .withMinute(0)
              .withSecond(0)
              .withNano(0)
              .toInstant();

      RefreshToken expectedRefreshToken =
          new RefreshToken(1L, VALID_REFRESH_TOKEN, VALID_USER, expectedExpiry, false);

      when(env.JWT_REFRESH_EXPIRATION_TIME_DAYS()).thenReturn(Duration.ofDays(7));
      when(env.AUTH_ENABLE_TIME_WINDOW()).thenReturn(true);
      when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(expectedRefreshToken);

      RefreshToken actualRefreshToken = refreshTokenService.createRefreshToken(VALID_USER);

      assertThat(actualRefreshToken).isEqualTo(expectedRefreshToken);

      verify(refreshTokenRepository)
          .save(argThat(token -> token.expiryDate().equals(expectedExpiry)));
    }

    @Test
    @DisplayName(
        "createRefreshToken: given time window is enabled and expiry is Before next Friday, then do not cap expiry")
    void createRefreshToken_windowEnabledAndExpiryBeforeNextFriday_doNotCapExpiry() {
      // Set the clock to a Tuesday 13 hs.
      // With a 1-day expiration, the token has to expire on Wednesday 13 hs.
      ZonedDateTime tuesday =
          LocalDateTime.of(2025, 11, 25, 10, 0).atZone(ZoneId.of("America/Montevideo"));
      when(clock.get()).thenReturn(tuesday);

      Instant expectedExpiry = tuesday.plusDays(1).toInstant();

      RefreshToken expectedRefreshToken =
          new RefreshToken(1L, VALID_REFRESH_TOKEN, VALID_USER, expectedExpiry, false);

      when(env.JWT_REFRESH_EXPIRATION_TIME_DAYS()).thenReturn(Duration.ofDays(1));
      when(env.AUTH_ENABLE_TIME_WINDOW()).thenReturn(true);
      when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(expectedRefreshToken);

      RefreshToken actualRefreshToken = refreshTokenService.createRefreshToken(VALID_USER);

      assertThat(actualRefreshToken).isEqualTo(expectedRefreshToken);

      verify(refreshTokenRepository)
          .save(argThat(token -> token.expiryDate().equals(expectedExpiry)));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName(
        "verifyAndRotate: given valid refresh token string, then return created refresh token")
    public void verifyAndRotate_validRefreshTokenString_returnCreatedRefreshToken(
        boolean useWindow) {
      RefreshToken oldRefreshToken =
          new RefreshToken(
              1L, VALID_REFRESH_TOKEN, VALID_USER, Instant.now().plusSeconds(2), false);
      RefreshToken newRefreshToken =
          new RefreshToken(
              2L, VALID_REFRESH_TOKEN, VALID_USER, Instant.now().plusSeconds(2), false);

      when(clock.get()).thenReturn(defaultClock.get());
      when(env.JWT_REFRESH_EXPIRATION_TIME_DAYS()).thenReturn(JWT_REFRESH_EXPIRATION_TIME_DAYS);
      when(env.AUTH_ENABLE_TIME_WINDOW()).thenReturn(useWindow);

      when(refreshTokenRepository.findByToken(VALID_REFRESH_TOKEN))
          .thenReturn(Optional.of(oldRefreshToken));

      when(refreshTokenRepository.save(oldRefreshToken.withRevoked(true)))
          .thenReturn(null); // do nothing

      when(refreshTokenRepository.save(argThat(token -> !token.revoked())))
          .thenReturn(newRefreshToken); // 2nd call

      RefreshToken actualRefreshToken = refreshTokenService.verifyAndRotate(VALID_REFRESH_TOKEN);
      assertThat(actualRefreshToken).isEqualTo(newRefreshToken);

      verify(refreshTokenRepository).save(oldRefreshToken.withRevoked(true));
      verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("cleanupExpiredTokens: given tokens exist, then return number of deleted tokens")
    public void cleanupExpiredTokens_tokensExist_returnNumberOfDeletedTokens() {
      int expectedDeletedTokens = 5;
      when(refreshTokenRepository.deleteExpiredTokens()).thenReturn(expectedDeletedTokens);
      int actualDeletedTokens = refreshTokenService.cleanupExpiredTokens();
      assertThat(actualDeletedTokens).isEqualTo(expectedDeletedTokens);
    }

    @Test
    @DisplayName("revokeToken: given existing token, then revoke token")
    public void revokeToken_existingToken_revokeToken() {
      RefreshToken token =
          new RefreshToken(
              1L, VALID_REFRESH_TOKEN, VALID_USER, Instant.now().plusSeconds(2), false);

      when(refreshTokenRepository.findByToken(VALID_REFRESH_TOKEN)).thenReturn(Optional.of(token));

      refreshTokenService.revokeToken(VALID_REFRESH_TOKEN);

      verify(refreshTokenRepository).save(token.withRevoked(true));
    }

    @Test
    @DisplayName("revokeToken: given non-existing token, then do nothing")
    public void revokeToken_nonExistingToken_doNothing() {
      when(refreshTokenRepository.findByToken(VALID_REFRESH_TOKEN)).thenReturn(Optional.empty());

      refreshTokenService.revokeToken(VALID_REFRESH_TOKEN);

      verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("revokeToken: given null token, then do nothing")
    public void revokeToken_nullToken_doNothing() {
      refreshTokenService.revokeToken(null);

      verify(refreshTokenRepository, never()).findByToken(any());
      verify(refreshTokenRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("Failure Scenarios")
  class FailureScenarios {
    @Test
    @DisplayName("createRefreshToken: given repository fails, then propagate thrown exception")
    public void createRefreshToken_repositoryFails_propagateThrownException() {
      when(clock.get()).thenReturn(defaultClock.get());
      when(env.JWT_REFRESH_EXPIRATION_TIME_DAYS()).thenReturn(JWT_REFRESH_EXPIRATION_TIME_DAYS);
      when(env.AUTH_ENABLE_TIME_WINDOW()).thenReturn(false);

      when(refreshTokenRepository.save(any(RefreshToken.class)))
          .thenThrow(PersistenceException.class);
      assertThatThrownBy(() -> refreshTokenService.createRefreshToken(null))
          .isInstanceOf(PersistenceException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "non-existing"})
    @DisplayName(
        "verifyAndRotate: given non-existing refresh token string, then throw InvalidCredentialsException")
    public void verifyAndRotate_nonExistingRefreshTokenString_throwInvalidCredentialsException(
        String cause) {
      String token = cause.equals("null") ? null : cause;

      when(refreshTokenRepository.findByToken(token)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> refreshTokenService.verifyAndRotate(token))
          .isInstanceOf(InvalidCredentialsException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"expired", "revoked"})
    @DisplayName(
        "verifyAndRotate: given invalid refresh token string, then throw InvalidCredentialsException")
    public void verifyAndRotate_invalidRefreshTokenString_throwInvalidCredentialsException(
        String cause) {
      RefreshToken validRefreshToken =
          new RefreshToken(
              1L,
              VALID_REFRESH_TOKEN,
              VALID_USER,
              Instant.now().plusSeconds(2),
              cause.equals("revoked"));
      if (cause.equals("expired")) {
        try {
          Thread.sleep(Duration.ofSeconds(3));
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }

      when(refreshTokenRepository.findByToken(VALID_REFRESH_TOKEN))
          .thenReturn(Optional.of(validRefreshToken));

      assertThatThrownBy(() -> refreshTokenService.verifyAndRotate(VALID_REFRESH_TOKEN))
          .isInstanceOf(InvalidCredentialsException.class)
          .hasMessage("Refresh token is expired or revoked");
    }

    @Test
    @DisplayName("verifyAndRotate: given repository fails, then propagate thrown exception")
    public void verifyAndRotate_repositoryFails_propagateThrownException() {
      when(refreshTokenRepository.findByToken(null)).thenThrow(PersistenceException.class);
      assertThatThrownBy(() -> refreshTokenService.verifyAndRotate(null))
          .isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("cleanupExpiredTokens: given repository fails, then propagate thrown exception")
    public void cleanupExpiredTokens_repositoryFails_propagateThrownException() {
      when(refreshTokenRepository.deleteExpiredTokens()).thenThrow(PersistenceException.class);
      assertThatThrownBy(() -> refreshTokenService.cleanupExpiredTokens())
          .isInstanceOf(PersistenceException.class);
    }
  }
}
