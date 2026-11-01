package com.insurtech.kernel.id;

import java.util.Objects;
import java.util.UUID;

/** Top-level isolation boundary: every record belongs to exactly one tenant (PRD-F01). */
public record TenantId(UUID value) {

  public TenantId {
    Objects.requireNonNull(value, "value");
  }

  public static TenantId of(String value) {
    return new TenantId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
