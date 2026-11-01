package com.insurtech.commissions.domain;

import java.util.Objects;
import java.util.UUID;

/** Identifier of a {@code CommissionPlan} aggregate (CRC-DOM-001). */
public record CommissionPlanId(UUID value) {

  public CommissionPlanId {
    Objects.requireNonNull(value, "value");
  }

  public static CommissionPlanId of(String value) {
    return new CommissionPlanId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
