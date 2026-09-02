package com.anibalxyz.features.auth.application;

import com.anibalxyz.core.Result;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SignatureException;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JwtService {
  private static final Logger log = LoggerFactory.getLogger(JwtService.class);
  private final Env env;
  private final Clock clock;

  public JwtService(Env env, Clock clock) {
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
        .expiration(Date.from(now.plusSeconds(env.JWT_ACCESS_EXPIRATION_TIME_SECONDS())))
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

  public interface Env {
    SecretKey JWT_KEY();

    String JWT_ISSUER();

    long JWT_ACCESS_EXPIRATION_TIME_SECONDS();
  }

  public sealed interface JwtValidationError {
    record Missing() implements JwtValidationError {}

    record Expired() implements JwtValidationError {}

    record Invalid() implements JwtValidationError {}
  }
}
