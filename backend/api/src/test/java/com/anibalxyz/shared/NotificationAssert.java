package com.anibalxyz.shared;

import static org.assertj.core.api.Assertions.assertThat;

import com.anibalxyz.core.application.ValidationNotification;
import com.anibalxyz.core.domain.error.DomainError;

/** Assertion helpers for {@link ValidationNotification}. */
public final class NotificationAssert {
  private final ValidationNotification<? extends DomainError> notification;

  private NotificationAssert(ValidationNotification<? extends DomainError> notification) {
    this.notification = notification;
  }

  public static NotificationAssert assertThatNotification(
      ValidationNotification<? extends DomainError> notification) {
    return new NotificationAssert(notification);
  }

  @SuppressWarnings("UnusedReturnValue")
  public NotificationAssert hasErrorOn(String field, Class<? extends DomainError> errorClass) {
    assertThat(notification.getErrors())
        .satisfiesExactly(
            e -> {
              assertThat(e.field()).isEqualTo(field);
              assertThat(e.error()).isInstanceOf(errorClass);
            });
    return this;
  }
}
