package com.insurtech.platform.application.audit;

import java.util.Objects;

/** Aggregate a command targeted, with its version before and after the change (CRC-AUD-002). */
public record AggregateRef(String type, String id, long versionBefore, long versionAfter) {

  public AggregateRef {
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(id, "id");
  }
}
