package com.anibalxyz.core.application;

import com.anibalxyz.core.Notification;
import com.anibalxyz.core.domain.error.DomainError;

public class ValidationNotification<E extends DomainError>
    extends Notification<ValidationNotification.ErrorEntry<E>> {

  public void add(String field, E error) {
    this.add(new ErrorEntry<>(field, error));
  }

  public boolean hasErrorFor(String field) {
    return errors.stream().anyMatch(e -> e.field().equals(field));
  }

  public record ErrorEntry<E>(String field, E error) {}
}
