package com.anibalxyz.features.auth.domain;

import com.anibalxyz.core.Result;
import com.anibalxyz.features.auth.domain.error.InvalidRawTokenError;
import java.util.Objects;
import java.util.UUID;

public final class RawToken {
  private final String value;

  private RawToken(String value) {
    this.value = value;
  }

  public static RawToken generate() {
    return new RawToken(UUID.randomUUID().toString());
  }

  public static Result<RawToken, InvalidRawTokenError> of(String raw) {
    if (!isValid(raw)) return Result.failure(new InvalidRawTokenError());
    return Result.success(new RawToken(raw));
  }

  public static boolean isValid(String token) {
    if (token == null || token.length() != 36) return false;

    boolean hyphensAreNotProperlyPlaced =
        token.charAt(8) != '-'
            || token.charAt(13) != '-'
            || token.charAt(18) != '-'
            || token.charAt(23) != '-';
    if (hyphensAreNotProperlyPlaced) return false;

    boolean isNotV4 = token.charAt(14) != '4';
    if (isNotV4) return false;

    char variant = token.charAt(19);
    boolean isInvalidVariant =
        variant != '8'
            && variant != '9'
            && variant != 'a'
            && variant != 'b'
            && variant != 'A'
            && variant != 'B';
    if (isInvalidVariant) return false;

    for (int i = 0; i < 36; i++) {
      if (i == 8 || i == 13 || i == 14 || i == 18 || i == 19 || i == 23) {
        continue;
      }

      char c = token.charAt(i);
      boolean isHexCharacter =
          (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
      if (!isHexCharacter) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(value);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof RawToken rawToken)) return false;
    return Objects.equals(value, rawToken.value);
  }

  public String value() {
    return value;
  }
}
