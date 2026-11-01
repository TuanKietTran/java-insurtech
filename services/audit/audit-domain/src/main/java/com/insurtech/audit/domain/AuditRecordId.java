package com.insurtech.audit.domain;

import java.util.Objects;
import java.util.UUID;

/** Identifier of a {@code AuditRecord} aggregate (CRC-DOM-001). */
public record AuditRecordId(UUID value) {

  public AuditRecordId {
    Objects.requireNonNull(value, "value");
  }

  public static AuditRecordId of(String value) {
    return new AuditRecordId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
