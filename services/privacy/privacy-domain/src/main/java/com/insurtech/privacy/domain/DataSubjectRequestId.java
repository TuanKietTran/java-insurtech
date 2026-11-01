package com.insurtech.privacy.domain;

import java.util.Objects;
import java.util.UUID;

/** Identifier of a {@code DataSubjectRequest} aggregate (CRC-DOM-001). */
public record DataSubjectRequestId(UUID value) {

  public DataSubjectRequestId {
    Objects.requireNonNull(value, "value");
  }

  public static DataSubjectRequestId of(String value) {
    return new DataSubjectRequestId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
