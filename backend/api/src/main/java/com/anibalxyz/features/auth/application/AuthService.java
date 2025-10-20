package com.anibalxyz.features.auth.application;

import com.anibalxyz.features.auth.api.in.LoginRequest;
import com.anibalxyz.features.auth.application.exception.InvalidCredentialsException;
import com.anibalxyz.features.common.application.exception.ResourceNotFoundException;
import com.anibalxyz.features.users.application.UserService;
import com.anibalxyz.features.users.domain.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

public class AuthService {
  private final Key key;
  private final String issuer;
  private final long expirationMinutes;

  private final UserService userService;

  public AuthService(AuthEnvironment env, UserService userService) {
    // TODO: check if this logic belongs here
    byte[] secretBytes = env.JWT_SECRET().getBytes(StandardCharsets.UTF_8);
    if (secretBytes.length < 32) {
      throw new IllegalArgumentException("JWT_SECRET must be at least 256 bits (32 bytes).");
    }

    this.key = Keys.hmacShaKeyFor(secretBytes);
    this.issuer = env.JWT_ISSUER();
    this.expirationMinutes = env.JWT_EXPIRATION_TIME().toMinutes();

    this.userService = userService;
  }

  public String generateToken(User user) {
    Instant now = Instant.now();
    String subject = String.valueOf(user.getId());

    return Jwts.builder()
        .id(UUID.randomUUID().toString())
        .subject(subject)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusSeconds(expirationMinutes * 60)))
        .issuer(issuer)
        .signWith(key)
        .compact();
  }

  public String authenticateUser(LoginRequest request) {
    try {
      User user = userService.getUserByEmail(request.email());
      if (user.getPasswordHash().matches(request.password())) {
        return generateToken(user);
      } else {
        throw new InvalidCredentialsException("Invalid credentials");
      }
    } catch (ResourceNotFoundException e) {
      throw new InvalidCredentialsException("Invalid credentials");
    }
  }
}
