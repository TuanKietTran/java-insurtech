package com.insurtech.notifications.domain;

import java.util.Objects;
import java.util.UUID;

/** Identifier of a {@code Communication} aggregate (CRC-DOM-001). */
public record CommunicationId(UUID value) {

  public CommunicationId {
    Objects.requireNonNull(value, "value");
  }

  public static CommunicationId of(String value) {
    return new CommunicationId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
