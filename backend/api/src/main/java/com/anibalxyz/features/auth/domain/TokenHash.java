package com.anibalxyz.features.auth.domain;

import com.anibalxyz.annotation.ExcludeFromJacocoGenerated;
import com.anibalxyz.server.exception.UnreachableCodeException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public final class TokenHash {
  private final byte[] value;

  private TokenHash(byte[] value) {
    this.value = value;
  }

  public static TokenHash of(RawToken rawToken) {
    MessageDigest messageDigest = getMessageDigest();
    String tokenString = rawToken.value();

    byte[] inputBytes = tokenString.getBytes(StandardCharsets.UTF_8);
    byte[] hashBytes = messageDigest.digest(inputBytes);
    return new TokenHash(hashBytes);
  }

  @ExcludeFromJacocoGenerated
  public static MessageDigest getMessageDigest() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw UnreachableCodeException.of(e, "SHA-256 algorithm not found");
    }
  }

  public static TokenHash reconstitute(byte[] value) {
    return new TokenHash(value);
  }

  public byte[] value() {
    return Arrays.copyOf(value, value.length);
  }

  @ExcludeFromJacocoGenerated
  @Override
  public int hashCode() {
    return Arrays.hashCode(value);
  }

  @ExcludeFromJacocoGenerated
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof TokenHash tokenHash)) return false;
    return Arrays.equals(value, tokenHash.value);
  }

  @ExcludeFromJacocoGenerated
  @Override
  public String toString() {
    return "*******";
  }
}
