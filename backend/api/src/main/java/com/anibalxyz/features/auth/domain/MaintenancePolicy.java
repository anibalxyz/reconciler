package com.anibalxyz.features.auth.domain;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

public final class MaintenancePolicy {
  public static final DayOfWeek START_DAY = DayOfWeek.FRIDAY;
  public static final LocalTime START_TIME = LocalTime.of(20, 0);
  public static final DayOfWeek END_DAY = DayOfWeek.MONDAY;
  public static final LocalTime END_TIME = LocalTime.of(8, 0);

  public Instant calculateExpiryDate(ZonedDateTime now, Duration expTimeDays) {
    Instant calculatedExpiry = now.plus(expTimeDays).toInstant();
    return capAtNextWindowStart(now, calculatedExpiry);
  }

  private Instant capAtNextWindowStart(ZonedDateTime now, Instant expiryDate) {
    Instant windowStart =
        now.with(TemporalAdjusters.nextOrSame(START_DAY)).with(START_TIME).toInstant();

    return expiryDate.isBefore(windowStart) ? expiryDate : windowStart;
  }

  public Optional<Instant> blockedUntil(ZonedDateTime now) {
    if (!isInMaintenanceWindow(now)) {
      return Optional.empty();
    }

    ZonedDateTime unblockTime =
        now.with(TemporalAdjusters.nextOrSame(END_DAY)).with(END_TIME).withNano(0);

    return Optional.of(unblockTime.toInstant());
  }

  private boolean isInMaintenanceWindow(ZonedDateTime now) {
    ZonedDateTime lastStart =
        now.with(TemporalAdjusters.previousOrSame(START_DAY)).with(START_TIME);
    ZonedDateTime nextEnd = lastStart.with(TemporalAdjusters.nextOrSame(END_DAY)).with(END_TIME);

    return now.isAfter(lastStart) && now.isBefore(nextEnd);
  }
}
