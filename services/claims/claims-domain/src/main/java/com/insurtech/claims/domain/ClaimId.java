package com.insurtech.claims.domain;

import java.util.Objects;
import java.util.UUID;

/** Identifier of a {@code Claim} aggregate (CRC-DOM-001). */
public record ClaimId(UUID value) {

  public ClaimId {
    Objects.requireNonNull(value, "value");
  }

  public static ClaimId of(String value) {
    return new ClaimId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
