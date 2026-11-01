package com.insurtech.kernel.id;

import java.util.Objects;
import java.util.UUID;

/** Identifier of an authenticated actor: human, service, external client, or AI Agent. */
public record PrincipalId(UUID value) {

  public PrincipalId {
    Objects.requireNonNull(value, "value");
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
