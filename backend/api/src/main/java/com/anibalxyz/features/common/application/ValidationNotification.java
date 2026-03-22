package com.anibalxyz.features.common.application;

import com.anibalxyz.features.common.Notification;
import com.anibalxyz.features.common.domain.error.ReasonedError;

public class ValidationNotification extends Notification<ValidationNotification.ErrorEntry> {
  public sealed interface FieldFailure {
    record Missing() implements FieldFailure {}

    record Blank() implements FieldFailure {}

    record Conflict() implements FieldFailure {}

    record InvalidValue(ReasonedError<?> error) implements FieldFailure {}
  }

  public record ErrorEntry(String field, FieldFailure failure) {}

  public void addMissing(String field) {
    this.add(new ErrorEntry(field, new FieldFailure.Missing()));
  }

  public void addBlank(String field) {
    this.add(new ErrorEntry(field, new FieldFailure.Blank()));
  }

  public void addConflict(String field) {
    this.add(new ErrorEntry(field, new FieldFailure.Conflict()));
  }

  public void add(String field, ReasonedError<?> error) {
    this.add(new ErrorEntry(field, new FieldFailure.InvalidValue(error)));
  }

  public boolean hasErrorFor(String field) {
    return errors.stream().anyMatch(e -> e.field().equals(field));
  }
}
