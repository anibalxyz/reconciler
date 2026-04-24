package com.anibalxyz.features.common.application;

import com.anibalxyz.features.common.Notification;
import com.anibalxyz.features.common.domain.error.DomainError;

public class ValidationNotification<E extends DomainError>
    extends Notification<ValidationNotification.ErrorEntry<E>> {

  public record ErrorEntry<E>(String field, E error) {}

  public void add(String field, E error) {
    this.add(new ErrorEntry<>(field, error));
  }

  public boolean hasErrorFor(String field) {
    return errors.stream().anyMatch(e -> e.field().equals(field));
  }
}
