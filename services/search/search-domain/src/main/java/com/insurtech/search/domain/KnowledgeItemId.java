package com.insurtech.search.domain;

import java.util.Objects;
import java.util.UUID;

/** Identifier of a {@code KnowledgeItem} aggregate (CRC-DOM-001). */
public record KnowledgeItemId(UUID value) {

  public KnowledgeItemId {
    Objects.requireNonNull(value, "value");
  }

  public static KnowledgeItemId of(String value) {
    return new KnowledgeItemId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
