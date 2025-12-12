package com.anibalxyz.features.auth.application;

import static com.anibalxyz.features.Constants.Environment.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.anibalxyz.features.Constants;
import com.anibalxyz.features.auth.application.env.JwtEnvironment;
import com.anibalxyz.features.auth.application.exception.InvalidCredentialsException;
import io.jsonwebtoken.Claims;
import java.time.Duration;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DisplayName("Tests for JwtService")
@ExtendWith(MockitoExtension.class)
public class JwtServiceTest {

  private static final Logger log = LoggerFactory.getLogger(JwtServiceTest.class);
  @Mock private JwtEnvironment env;
  @InjectMocks private JwtService jwtService;

  @BeforeAll
  public static void init() {
    Constants.init();
  }

  private static JwtEnvironment createEnv(
      String jwtSecret, String jwtIssuer, long jwtExpirationTime, SecretKey jwtKey) {
    return new JwtEnvironment() {
      @Override
      public String JWT_SECRET() {
        return jwtSecret;
      }

      @Override
      public SecretKey JWT_KEY() {
        return jwtKey; // TODO: this is temporal and should be removed before commit
      }

      @Override
      public String JWT_ISSUER() {
        return jwtIssuer;
      }

      @Override
      public long JWT_ACCESS_EXPIRATION_TIME_MINUTES() {
        return jwtExpirationTime;
      }
    };
  }

  @BeforeEach
  public void setup() {
    lenient().when(env.JWT_SECRET()).thenReturn(JWT_SECRET);
    lenient().when(env.JWT_ISSUER()).thenReturn(JWT_ISSUER);
    lenient()
        .when(env.JWT_ACCESS_EXPIRATION_TIME_MINUTES())
        .thenReturn(JWT_ACCESS_EXPIRATION_TIME_MINUTES);
    lenient().when(env.JWT_KEY()).thenReturn(JWT_KEY);
  }

  @Nested
  @DisplayName("Success Scenarios")
  class SuccessScenarios {
    @Test
    @DisplayName("generateToken: given a valid user ID, then return a non-null token")
    public void generateToken_validUserId_returnNonNullToken() {
      Integer userId = 123;

      String token = jwtService.generateToken(userId);

      assertNotNull(token);
    }

    @Test
    @DisplayName("validateToken: given a valid token, then return valid claims")
    public void validateToken_validToken_returnValidClaims() {

      Integer userId = 123;
      String token = jwtService.generateToken(userId);

      Claims claims = assertDoesNotThrow(() -> jwtService.validateToken(token));

      assertEquals(String.valueOf(userId), claims.getSubject());
      assertEquals(JWT_ISSUER, claims.getIssuer());
    }
  }

  @Nested
  @DisplayName("Failure Scenarios")
  class FailureScenarios {
    @Test
    @DisplayName("validateToken: given an expired token, then throw InvalidCredentialsException")
    public void validateToken_expiredToken_throwInvalidCredentialsException() {
      when(env.JWT_ACCESS_EXPIRATION_TIME_MINUTES()).thenReturn(Duration.ofSeconds(1).toMinutes());

      Integer userId = 123;
      String token = jwtService.generateToken(userId);

      // Wait for the token to expire
      try {
        Thread.sleep(1500); // Sleep for 1.5 seconds
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }

      assertThatThrownBy(() -> jwtService.validateToken(token))
          .isInstanceOf(InvalidCredentialsException.class)
          .hasMessage("JWT has expired");
    }

    @Test
    @DisplayName("validateToken: given a malformed token, then throw InvalidCredentialsException")
    public void validateToken_malformedToken_throwInvalidCredentialsException() {

      String malformedToken = "this.is.not.a.valid.jwt";

      assertThatThrownBy(() -> jwtService.validateToken(malformedToken))
          .isInstanceOf(InvalidCredentialsException.class)
          .hasMessage("Invalid JWT token");
    }

    @Test
    @DisplayName(
        "validateToken: given a token with invalid signature, then throw InvalidCredentialsException")
    public void validateToken_invalidSignatureToken_throwInvalidCredentialsException() {
      JwtEnvironment differentSecretEnv =
          createEnv(
              "another_secret_greather_than_32_bytes_for_testing",
              JWT_ISSUER,
              JWT_ACCESS_EXPIRATION_TIME_MINUTES,
              JWT_KEY);

      JwtService jwtServiceWithDifferentSecret = new JwtService(differentSecretEnv);
      Integer userId = 123;
      String token = jwtService.generateToken(userId); // Signed with original secret

      assertThatThrownBy(() -> jwtServiceWithDifferentSecret.validateToken(token))
          .isInstanceOf(InvalidCredentialsException.class)
          .hasMessage("Invalid JWT token");
    }

    @ParameterizedTest
    @NullAndEmptySource 
    @ValueSource(strings = {" "})
    @DisplayName(
        "validateToken: given a null or empty token, then throw InvalidCredentialsException")
    public void validateToken_nullOrEmptyToken_throwInvalidCredentialsException(String invalidToken) {
      assertThatThrownBy(() -> jwtService.validateToken(invalidToken))
          .isInstanceOf(InvalidCredentialsException.class)
          .hasMessage("Invalid JWT token");
    }
  }
}
