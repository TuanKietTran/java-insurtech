package com.insurtech.fraud.domain;

import java.util.Objects;
import java.util.UUID;

/** Identifier of a {@code Signal} aggregate (CRC-DOM-001). */
public record SignalId(UUID value) {

  public SignalId {
    Objects.requireNonNull(value, "value");
  }

  public static SignalId of(String value) {
    return new SignalId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
