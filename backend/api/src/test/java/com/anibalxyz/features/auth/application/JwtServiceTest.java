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
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import javax.crypto.SecretKey;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("Tests for JwtService")
@ExtendWith(MockitoExtension.class)
public class JwtServiceTest {

  @Mock private JwtEnvironment env;
  @InjectMocks private JwtService jwtService;

  @BeforeAll
  public static void init() {
    Constants.init();
    Assertions.setMaxStackTraceElementsDisplayed(120);
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
      // NOTE: the hand-made service uses default environment-injected values while the mocked one
      //       changes the signature
      JwtService jwtServiceWithDifferentSignature = new JwtService(Constants.APP_CONFIG.env());

      byte[] differentSecretBytes =
          Constants.APP_CONFIG
              .env()
              .JWT_SECRET()
              .concat("makeItDifferent")
              .getBytes(StandardCharsets.UTF_8);
      SecretKey differentKey = Keys.hmacShaKeyFor(differentSecretBytes);
      when(env.JWT_KEY()).thenReturn(differentKey);

      Integer userId = 123;
      String token = jwtServiceWithDifferentSignature.generateToken(userId);

      assertThatThrownBy(() -> jwtService.validateToken(token))
          .isInstanceOf(InvalidCredentialsException.class)
          .hasMessage("Invalid JWT token");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName(
        "validateToken: given a null or empty token, then throw InvalidCredentialsException")
    public void validateToken_nullOrEmptyToken_throwInvalidCredentialsException(
        String invalidToken) {
      assertThatThrownBy(() -> jwtService.validateToken(invalidToken))
          .isInstanceOf(InvalidCredentialsException.class)
          .hasMessage("Invalid JWT token");
    }
  }
}
