package com.anibalxyz.features.auth.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.anibalxyz.features.auth.application.env.JwtEnvironment;
import com.anibalxyz.core.Result;
import com.anibalxyz.shared.Constants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.*;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("Tests for JwtService")
@ExtendWith(MockitoExtension.class)
class JwtServiceTest {
  private static final ZonedDateTime FIXED_NOW =
      LocalDateTime.of(2025, 11, 25, 10, 0).atZone(ZoneId.of("America/Montevideo"));
  private static final Clock testClock = Clock.fixed(FIXED_NOW.toInstant(), FIXED_NOW.getZone());
  private static final String JWT_SECRET = "some_secret_greather_than_32_bytes";
  private static final SecretKey JWT_KEY =
      Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
  private static final String JWT_ISSUER = "test-issuer";
  private static final long JWT_EXPIRATION_MINUTES = 30L;
  private static final int USER_ID = 123;
  private static final JwtEnvironmentStub env;

  static {
    // placed here to avoid error caused by bad order of declaration
    env = new JwtEnvironmentStub(JWT_KEY, JWT_ISSUER, JWT_EXPIRATION_MINUTES);
  }

  private JwtService jwtService;

  @BeforeAll
  static void init() {
    Constants.init();
  }

  @BeforeEach
  void setup() {
    jwtService = new JwtService(env, testClock);
  }

  @Test
  @DisplayName("generateToken: given valid user ID, then return valid jwt")
  void generateToken_validUserId_returnValidJwt() {
    String jwt = jwtService.generateToken(USER_ID);
    Result<Claims, JwtService.JwtValidationError> validationResult = jwtService.validateToken(jwt);
    assertThat(validationResult.isSuccess()).isTrue();
  }

  @Test
  @DisplayName("validateToken: given valid token, then return success with correct claims")
  void validateToken_validToken_returnSuccessWithCorrectClaims() {
    String token = jwtService.generateToken(USER_ID);

    Result<Claims, JwtService.JwtValidationError> result = jwtService.validateToken(token);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getValue().getSubject()).isEqualTo(String.valueOf(USER_ID));
    assertThat(result.getValue().getIssuer()).isEqualTo(JWT_ISSUER);
  }

  @Test
  @DisplayName("validateToken: given expired token, then return failure with Expired reason")
  void validateToken_expiredToken_returnFailureWithExpired() {
    JwtService delayedService = new JwtService(env, Clock.offset(testClock, Duration.ofHours(-50)));
    String token = delayedService.generateToken(USER_ID);

    Result<Claims, JwtService.JwtValidationError> result = jwtService.validateToken(token);

    assertThat(result.isFailure()).isTrue();
    assertThat(result.getError()).isInstanceOf(JwtService.JwtValidationError.Expired.class);
  }

  @Test
  @DisplayName("validateToken: given premature token, then return failure with Invalid reason")
  void validateToken_prematureToken_returnFailureWithInvalid() {
    JwtService delayedService = new JwtService(env, Clock.offset(testClock, Duration.ofHours(50)));
    String token = delayedService.generateToken(USER_ID);

    Result<Claims, JwtService.JwtValidationError> result = jwtService.validateToken(token);

    assertThat(result.isFailure()).isTrue();
    assertThat(result.getError()).isInstanceOf(JwtService.JwtValidationError.Invalid.class);
  }

  @Test
  @DisplayName("validateToken: given malformed token, then return failure with Invalid reason")
  void validateToken_malformedToken_returnFailureWithInvalid() {
    Result<Claims, JwtService.JwtValidationError> result =
        jwtService.validateToken("this.is.not.a.valid.jwt...");

    assertThat(result.isFailure()).isTrue();
    assertThat(result.getError()).isInstanceOf(JwtService.JwtValidationError.Invalid.class);
  }

  @Test
  @DisplayName(
      "validateToken: given token with invalid signature, then return failure with Invalid reason")
  void validateToken_invalidSignature_returnFailureWithInvalid() {
    // Must be the same size (or less) as JWT_SECRET
    // NOTE: replace with a different char. Currently: "s" -> "^".
    String stringToMakeItDifferent = JWT_SECRET.substring(0, JWT_SECRET.length() - 1) + "^";
    SecretKey differentKey =
        Keys.hmacShaKeyFor((stringToMakeItDifferent).getBytes(StandardCharsets.UTF_8));
    JwtService serviceWithDifferentKey = new JwtService(env.withJWT_KEY(differentKey), testClock);

    // Token signed with differentKey, validated by jwtService (which uses JWT_KEY)
    String token = serviceWithDifferentKey.generateToken(USER_ID);
    Result<Claims, JwtService.JwtValidationError> result = jwtService.validateToken(token);

    assertThat(result.isFailure()).isTrue();
    assertThat(result.getError()).isInstanceOf(JwtService.JwtValidationError.Invalid.class);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" "})
  @DisplayName(
      "validateToken: given null, empty or blank token, then return failure with Missing reason")
  void validateToken_nullOrBlankToken_returnFailureWithMissing(String token) {
    Result<Claims, JwtService.JwtValidationError> result = jwtService.validateToken(token);

    assertThat(result.isFailure()).isTrue();
    assertThat(result.getError()).isInstanceOf(JwtService.JwtValidationError.Missing.class);
  }

  private record JwtEnvironmentStub(
      SecretKey JWT_KEY, String JWT_ISSUER, long JWT_ACCESS_EXPIRATION_TIME_MINUTES)
      implements JwtEnvironment {
    public JwtEnvironmentStub withJWT_KEY(SecretKey JWT_KEY) {
      return new JwtEnvironmentStub(
          JWT_KEY, this.JWT_ISSUER, this.JWT_ACCESS_EXPIRATION_TIME_MINUTES);
    }
  }
}
