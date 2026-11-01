package com.insurtech.aiharness.domain;

import java.util.Objects;
import java.util.UUID;

/** Identifier of a {@code AiAgentDefinition} aggregate (CRC-DOM-001). */
public record AiAgentDefinitionId(UUID value) {

  public AiAgentDefinitionId {
    Objects.requireNonNull(value, "value");
  }

  public static AiAgentDefinitionId of(String value) {
    return new AiAgentDefinitionId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
