package com.insurtech.rules.domain;

import java.util.Objects;
import java.util.UUID;

/** Identifier of a {@code RuleSet} aggregate (CRC-DOM-001). */
public record RuleSetId(UUID value) {

  public RuleSetId {
    Objects.requireNonNull(value, "value");
  }

  public static RuleSetId of(String value) {
    return new RuleSetId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
