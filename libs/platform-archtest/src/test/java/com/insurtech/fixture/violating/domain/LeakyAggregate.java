package com.insurtech.fixture.violating.domain;

import java.time.Instant;

/** Fixture: violates CRC-DOM-003 (setter), CRC-MON-001 (double) and CRC-TIME-002 (clock). */
public final class LeakyAggregate {

  private double premium;
  private Instant updatedAt = Instant.EPOCH;

  public void setPremium(double premium) {
    this.premium = premium;
    this.updatedAt = Instant.now();
  }

  public double premium() {
    return premium;
  }

  public Instant updatedAt() {
    return updatedAt;
  }
}
