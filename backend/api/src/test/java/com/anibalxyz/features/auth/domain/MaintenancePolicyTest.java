package com.anibalxyz.features.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.anibalxyz.shared.UnitTest;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class MaintenancePolicyTest extends UnitTest {
  private static final ZoneId ZONE = ZoneId.of("America/Montevideo");
  private static final MaintenancePolicy maintenancePolicy = new MaintenancePolicy();
  private static final ZonedDateTime BASE_DATE = ZonedDateTime.of(2026, 4, 20, 0, 0, 0, 0, ZONE);
  private static final ZonedDateTime WINDOW_START =
      BASE_DATE
          .with(TemporalAdjusters.nextOrSame(MaintenancePolicy.START_DAY))
          .with(MaintenancePolicy.START_TIME);
  private static final ZonedDateTime WINDOW_END = calculateWindowEnd();
  private static final ZonedDateTime VALID_TIME = WINDOW_START.minusHours(5);

  private static ZonedDateTime calculateWindowEnd() {
    ZonedDateTime end =
        MaintenancePolicyTest.WINDOW_START
            .with(TemporalAdjusters.nextOrSame(MaintenancePolicy.END_DAY))
            .with(MaintenancePolicy.END_TIME);

    if (!end.isAfter(MaintenancePolicyTest.WINDOW_START)) {
      end = end.plusWeeks(1);
    }
    return end;
  }

  @Nested
  @DisplayName("Tests for Expiry Policy logic")
  class TokenExpiryPolicy {
    @Test
    @DisplayName("calculateExpiryDate: given expiry before Window Start, then return normal expiry")
    void calculateExpiryDate_expiryBeforeWindowStart_returnNormal() {
      Duration exp = Duration.ofHours(2);
      Instant result = maintenancePolicy.calculateExpiryDate(VALID_TIME, exp);

      assertThat(result).isEqualTo(VALID_TIME.plus(exp).toInstant());
    }

    @Test
    @DisplayName("calculateExpiryDate: given expiry after Window Start, then cap at Window Start")
    void calculateExpiryDate_expiryAfterWindowStart_capAtWindowStart() {
      Duration exp = Duration.ofDays(10);
      Instant result = maintenancePolicy.calculateExpiryDate(VALID_TIME, exp);

      assertThat(result).isEqualTo(WINDOW_START.toInstant());
    }

    @Test
    @DisplayName(
        "calculateExpiryDate: given now is past Window Start, then return Window Start (past limit)")
    void calculateExpiryDate_nowIsAfterWindowStart_returnPastLimit() {
      ZonedDateTime now = WINDOW_START.plusHours(2);
      Duration exp = Duration.ofHours(5);

      Instant result = maintenancePolicy.calculateExpiryDate(now, exp);

      assertThat(result).isEqualTo(WINDOW_START.toInstant());
      assertThat(result).isBefore(now.toInstant());
    }

    @Test
    @DisplayName(
        "calculateExpiryDate: given now is exactly Window Start, then return that same instant")
    void calculateExpiryDate_exactlyAtLimit_returnSameInstant() {
      Duration exp = Duration.ofDays(1);
      Instant result = maintenancePolicy.calculateExpiryDate(WINDOW_START, exp);

      assertThat(result).isEqualTo(WINDOW_START.toInstant());
    }
  }

  @Nested
  @DisplayName("Tests for System Access Policy logic")
  class SystemAccessPolicy {

    @Test
    @DisplayName("blockedUntil: given now is a valid time outside maintenance, then return empty")
    void blockedUntil_validTime_returnEmpty() {
      assertThat(maintenancePolicy.blockedUntil(VALID_TIME)).isEmpty();
    }

    @Test
    @DisplayName("blockedUntil: exactly at Window Start should still be open (empty)")
    void blockedUntil_exactlyAtWindowStart_returnEmpty() {
      assertThat(maintenancePolicy.blockedUntil(WINDOW_START)).isEmpty();
    }

    @Test
    @DisplayName("blockedUntil: given now is right after Window Start, then return Window End")
    void blockedUntil_rightAfterWindowStart_returnWindowEnd() {
      ZonedDateTime now = WINDOW_START.plusMinutes(1);
      assertThat(maintenancePolicy.blockedUntil(now)).contains(WINDOW_END.toInstant());
    }

    @Test
    @DisplayName("blockedUntil: given now is deep inside the window, then return Window End")
    void blockedUntil_deepInsideWindow_returnWindowEnd() {
      ZonedDateTime midWindow1 = WINDOW_START.plusHours(12);
      ZonedDateTime midWindow2 = WINDOW_END.minusHours(12);

      assertThat(maintenancePolicy.blockedUntil(midWindow1)).contains(WINDOW_END.toInstant());
      assertThat(maintenancePolicy.blockedUntil(midWindow2)).contains(WINDOW_END.toInstant());
    }

    @Test
    @DisplayName("blockedUntil: given now is right before Window End, then return Window End")
    void blockedUntil_rightBeforeWindowEnd_returnWindowEnd() {
      ZonedDateTime now = WINDOW_END.minusMinutes(1);
      assertThat(maintenancePolicy.blockedUntil(now)).contains(WINDOW_END.toInstant());
    }

    @Test
    @DisplayName("blockedUntil: exactly at Window End should be open (empty)")
    void blockedUntil_exactlyAtWindowEnd_returnEmpty() {
      assertThat(maintenancePolicy.blockedUntil(WINDOW_END)).isEmpty();
    }
  }
}
