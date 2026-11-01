package com.insurtech.policy.domain;

import java.util.Objects;
import java.util.UUID;

/** Identifier of a {@code PolicyTerm} aggregate (CRC-DOM-001). */
public record PolicyTermId(UUID value) {

  public PolicyTermId {
    Objects.requireNonNull(value, "value");
  }

  public static PolicyTermId of(String value) {
    return new PolicyTermId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
