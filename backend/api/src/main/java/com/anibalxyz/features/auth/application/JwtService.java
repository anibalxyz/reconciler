package com.anibalxyz.features.auth.application;

import com.anibalxyz.features.auth.application.env.JwtEnvironment;
import com.anibalxyz.features.common.Result;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SignatureException;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JwtService {
  private static final Logger log = LoggerFactory.getLogger(JwtService.class);
  private final JwtEnvironment env;
  private final Clock clock;

  public JwtService(JwtEnvironment env, Clock clock) {
    this.env = env;
    this.clock = clock;
  }

  public String generateToken(Integer userId) {
    String subject = String.valueOf(userId);
    Instant now = clock.instant();
    Date iat = Date.from(now);

    return Jwts.builder()
        .id(UUID.randomUUID().toString())
        .subject(subject)
        .issuedAt(iat)
        .notBefore(iat)
        // TODO: move this calculation to ConfigurationFactory
        .expiration(Date.from(now.plusSeconds(env.JWT_ACCESS_EXPIRATION_TIME_MINUTES() * 60)))
        .issuer(env.JWT_ISSUER())
        .signWith(env.JWT_KEY())
        .compact();
  }

  public Result<Claims, JwtValidationError> validateToken(String token) {
    if (token == null || token.isBlank()) {
      return Result.failure(new JwtValidationError.Missing());
    }
    try {
      return Result.success(
          Jwts.parser()
              .verifyWith(env.JWT_KEY())
              .clock(() -> Date.from(clock.instant()))
              .build()
              .parseSignedClaims(token)
              .getPayload());
    } catch (ExpiredJwtException e) {
      return Result.failure(new JwtValidationError.Expired());
    } catch (SignatureException e) {
      log.warn("Security Alert: Invalid signature detected");
      return Result.failure(new JwtValidationError.Invalid());
    } catch (PrematureJwtException | MalformedJwtException | UnsupportedJwtException e) {
      log.warn("Security Alert: Invalid token detected - {}", e.toString());
      return Result.failure(new JwtValidationError.Invalid());
    }
  }

  public sealed interface JwtValidationError {
    record Missing() implements JwtValidationError {}

    record Expired() implements JwtValidationError {}

    record Invalid() implements JwtValidationError {}
  }
}
