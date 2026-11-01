package com.insurtech.billing.domain;

import java.util.Objects;
import java.util.UUID;

/** Identifier of a {@code BillingAccount} aggregate (CRC-DOM-001). */
public record BillingAccountId(UUID value) {

  public BillingAccountId {
    Objects.requireNonNull(value, "value");
  }

  public static BillingAccountId of(String value) {
    return new BillingAccountId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
