package com.insurtech.regreporting.domain;

import java.util.Objects;
import java.util.UUID;

/** Identifier of a {@code ReportRun} aggregate (CRC-DOM-001). */
public record ReportRunId(UUID value) {

  public ReportRunId {
    Objects.requireNonNull(value, "value");
  }

  public static ReportRunId of(String value) {
    return new ReportRunId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
