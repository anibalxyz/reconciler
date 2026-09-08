package com.anibalxyz.features.auth.application;

import com.anibalxyz.core.primitives.Result;
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
  private final Settings settings;
  private final Clock clock;

  public JwtService(Settings settings, Clock clock) {
    this.settings = settings;
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
        .expiration(Date.from(now.plusSeconds(settings.jwtAccessExpirationTimeSeconds())))
        .issuer(settings.jwtIssuer())
        .signWith(settings.jwtKey())
        .compact();
  }

  public Result<Claims, JwtValidationError> validateToken(String token) {
    if (token == null || token.isBlank()) {
      return Result.failure(new JwtValidationError.Missing());
    }
    try {
      return Result.success(
          Jwts.parser()
              .verifyWith(settings.jwtKey())
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

  public interface Settings {
    SecretKey jwtKey();

    String jwtIssuer();

    long jwtAccessExpirationTimeSeconds();
  }

  public sealed interface JwtValidationError {
    record Missing() implements JwtValidationError {}

    record Expired() implements JwtValidationError {}

    record Invalid() implements JwtValidationError {}
  }
}
