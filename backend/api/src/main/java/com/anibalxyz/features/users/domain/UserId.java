package com.anibalxyz.features.users.domain;

import com.anibalxyz.core.Result;
import com.anibalxyz.features.users.domain.error.InvalidUserIdError;

import java.util.Objects;

public class UserId {
  private final Integer value;

  private UserId(Integer value) {
    this.value = value;
  }

  public static Result<UserId, InvalidUserIdError> of(Integer value) {
    if (value == null || value <= 0) return Result.failure(new InvalidUserIdError());
    return Result.success(new UserId(value));
  }

  public Integer value() {
    return this.value;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(value);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof UserId userId)) return false;
    return Objects.equals(value, userId.value);
  }

  @Override
  public String toString() {
    return "UserId{" + "value=" + value + '}';
  }
}
