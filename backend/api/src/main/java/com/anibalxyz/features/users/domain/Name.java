package com.anibalxyz.features.users.domain;

import com.anibalxyz.annotation.ExcludeFromJacocoGenerated;
import com.anibalxyz.core.Result;
import com.anibalxyz.features.users.domain.error.InvalidNameError;
import java.util.Objects;

public final class Name {
  public static final int MAX_LENGTH = 100;
  private final String value;

  private Name(String value) {
    this.value = value;
  }

  public static Result<Name, InvalidNameError> of(String value) {
    return validate(value).map(v -> new Name(value));
  }

  public static Result<Void, InvalidNameError> validate(String value) {
    if (value == null) return Result.failure(InvalidNameError.absent());
    if (value.length() > MAX_LENGTH) return Result.failure(InvalidNameError.tooLong(MAX_LENGTH));
    if (value.isBlank()) return Result.failure(InvalidNameError.blank());

    return Result.success();
  }

  public String value() {
    return value;
  }

  @Override
  @ExcludeFromJacocoGenerated
  public int hashCode() {
    return Objects.hashCode(value);
  }

  @Override
  @ExcludeFromJacocoGenerated
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Name other)) return false;
    return Objects.equals(value, other.value);
  }

  @Override
  @ExcludeFromJacocoGenerated
  public String toString() {
    return value;
  }
}
