package com.insurtech.rating.domain;

import java.util.Objects;
import java.util.UUID;

/** Identifier of a {@code RatingPlan} aggregate (CRC-DOM-001). */
public record RatingPlanId(UUID value) {

  public RatingPlanId {
    Objects.requireNonNull(value, "value");
  }

  public static RatingPlanId of(String value) {
    return new RatingPlanId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
