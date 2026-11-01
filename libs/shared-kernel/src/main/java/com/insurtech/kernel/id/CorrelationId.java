package com.insurtech.kernel.id;

import java.util.Objects;
import java.util.UUID;

/** Ties together every command, event and audit record of one business flow (CRC-OBS-001). */
public record CorrelationId(UUID value) {

  public CorrelationId {
    Objects.requireNonNull(value, "value");
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
