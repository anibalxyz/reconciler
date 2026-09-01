package com.anibalxyz.shared;

import com.anibalxyz.features.auth.domain.MaintenancePolicy;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;

public final class MaintenanceTestClock {
  public static final ZoneId ZONE = ZoneId.of("America/Montevideo");

  public static final ZonedDateTime BASE_DATE = ZonedDateTime.of(2026, 4, 20, 0, 0, 0, 0, ZONE);

  public static final ZonedDateTime WINDOW_START =
      BASE_DATE
          .with(TemporalAdjusters.nextOrSame(MaintenancePolicy.START_DAY))
          .with(MaintenancePolicy.START_TIME);

  public static final ZonedDateTime WINDOW_END =
      WINDOW_START
          .with(TemporalAdjusters.nextOrSame(MaintenancePolicy.END_DAY))
          .with(MaintenancePolicy.END_TIME)
          .withNano(0);

  public static final ZonedDateTime INSIDE_WINDOW_TIME = WINDOW_START.plusHours(2);

  public static final ZonedDateTime OUTSIDE_WINDOW_TIME = WINDOW_START.minusHours(5);

  private MaintenanceTestClock() {}
}
