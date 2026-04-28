package com.anibalxyz.shared;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

public class MutableClock extends Clock {
  private final AtomicReference<Instant> instant;
  private final ZoneId zoneId;

  public MutableClock(Instant instant, ZoneId zoneId) {
    this.instant = new AtomicReference<>(instant);
    this.zoneId = zoneId;
  }

  public void advanceBy(Duration duration) {
    instant.updateAndGet(current -> current.plus(duration));
  }

  public void moveTo(Instant newInstant) {
    instant.set(newInstant);
  }

  public void resetTo(Instant pointInTime) {
    instant.set(pointInTime);
  }

  @Override
  public ZoneId getZone() {
    return zoneId;
  }

  @Override
  public Clock withZone(ZoneId zone) {
    return new MutableClock(instant.get(), zone);
  }

  @Override
  public Instant instant() {
    return instant.get();
  }
}
