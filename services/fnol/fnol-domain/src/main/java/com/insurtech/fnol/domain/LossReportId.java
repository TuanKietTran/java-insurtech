package com.insurtech.fnol.domain;

import java.util.Objects;
import java.util.UUID;

/** Identifier of a {@code LossReport} aggregate (CRC-DOM-001). */
public record LossReportId(UUID value) {

  public LossReportId {
    Objects.requireNonNull(value, "value");
  }

  public static LossReportId of(String value) {
    return new LossReportId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
