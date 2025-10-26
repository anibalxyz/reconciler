package com.anibalxyz.features.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import com.anibalxyz.features.auth.application.env.RefreshTokenEnvironment;
import com.anibalxyz.features.auth.application.exception.InvalidCredentialsException;
import com.anibalxyz.features.auth.domain.RefreshToken;
import com.anibalxyz.features.auth.domain.RefreshTokenRepository;
import com.anibalxyz.features.users.domain.Email;
import com.anibalxyz.features.users.domain.PasswordHash;
import com.anibalxyz.features.users.domain.User;
import com.anibalxyz.server.config.environment.ConfigurationFactory;
import jakarta.persistence.PersistenceException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RefreshTokenServiceTest {
  private static final String VALID_REFRESH_TOKEN = "e4192c47-9649-48be-9f88-523240f45b6e";
  private static final User VALID_USER =
      new User(
          1,
          "Jhon Doe",
          new Email("validEmail@email.com"),
          PasswordHash.generate("validPassword", 10),
          Instant.now(),
          Instant.now());

  @Mock private static RefreshTokenRepository refreshTokenRepository;
  private static RefreshTokenEnvironment env;

  @InjectMocks private RefreshTokenService refreshTokenService;

  @BeforeAll
  public static void setup() {
    env = ConfigurationFactory.loadForTest().env();
  }

  @BeforeEach
  public void di() {
    refreshTokenService = new RefreshTokenService(refreshTokenRepository, env);
  }

  private RefreshTokenEnvironment createEnv(Duration jwtRefreshExpirationTime) {
    return () -> jwtRefreshExpirationTime;
  }

  @Nested
  class SuccessfulScenarios {
    @Test
    @DisplayName("createRefreshToken: given valid user, then return refresh token")
    public void createRefreshToken_validUser_returnRefreshToken() {
      RefreshToken expectedRefreshToken =
          new RefreshToken(
              1L, VALID_REFRESH_TOKEN, VALID_USER, Instant.now().plusSeconds(2), false);

      when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(expectedRefreshToken);

      RefreshToken actualRefreshToken = refreshTokenService.createRefreshToken(VALID_USER);
      assertThat(actualRefreshToken).isEqualTo(expectedRefreshToken);
    }

    @Test
    @DisplayName("verifyAndRotate: given valid refresh token string, then return created refresh token")
    public void verifyAndRotate_validRefreshTokenString_returnCreatedRefreshToken() {
      RefreshToken oldRefreshToken =
          new RefreshToken(
              1L, VALID_REFRESH_TOKEN, VALID_USER, Instant.now().plusSeconds(2), false);
      RefreshToken newRefreshToken =
          new RefreshToken(
              2L, VALID_REFRESH_TOKEN, VALID_USER, Instant.now().plusSeconds(2), false);

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
  }

  @Nested
  class FailureScenarios {
    @Test
    @DisplayName("createRefreshToken: given repository fails, then propagate thrown exception")
    public void createRefreshToken_repositoryFails_propagateThrownException() {
      when(refreshTokenRepository.save(any(RefreshToken.class)))
          .thenThrow(PersistenceException.class);
      assertThatThrownBy(() -> refreshTokenService.createRefreshToken(null))
          .isInstanceOf(PersistenceException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "non-existing"})
    @DisplayName("verifyAndRotate: given non-existing refresh token string, then throw InvalidCredentialsException")
    public void verifyAndRotate_nonExistingRefreshTokenString_throwInvalidCredentialsException(
        String cause) {
      String token = cause.equals("null") ? null : cause;

      when(refreshTokenRepository.findByToken(token)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> refreshTokenService.verifyAndRotate(token))
          .isInstanceOf(InvalidCredentialsException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"expired", "revoked"})
    @DisplayName("verifyAndRotate: given invalid refresh token string, then throw InvalidCredentialsException")
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
