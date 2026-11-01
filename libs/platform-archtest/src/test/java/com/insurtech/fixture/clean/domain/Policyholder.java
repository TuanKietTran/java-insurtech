package com.insurtech.fixture.clean.domain;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/** Fixture: contract-compliant domain class. */
public final class Policyholder {

  private final String name;
  private BigDecimal riskScore;
  private Instant reviewedAt;

  public Policyholder(String name, BigDecimal riskScore, Clock clock) {
    this.name = Objects.requireNonNull(name);
    this.riskScore = riskScore;
    this.reviewedAt = Instant.now(clock);
  }

  public void rescore(BigDecimal newScore, Clock clock) {
    this.riskScore = newScore;
    this.reviewedAt = Instant.now(clock);
  }

  public String name() {
    return name;
  }

  public BigDecimal riskScore() {
    return riskScore;
  }

  public Instant reviewedAt() {
    return reviewedAt;
  }
}
