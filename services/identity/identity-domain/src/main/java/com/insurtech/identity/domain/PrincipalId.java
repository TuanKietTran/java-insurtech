package com.insurtech.identity.domain;

import java.util.Objects;
import java.util.UUID;

/** Identifier of a {@code Principal} aggregate (CRC-DOM-001). */
public record PrincipalId(UUID value) {

  public PrincipalId {
    Objects.requireNonNull(value, "value");
  }

  public static PrincipalId of(String value) {
    return new PrincipalId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
