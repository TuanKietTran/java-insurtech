package com.insurtech.documents.domain;

import java.util.Objects;
import java.util.UUID;

/** Identifier of a {@code Document} aggregate (CRC-DOM-001). */
public record DocumentId(UUID value) {

  public DocumentId {
    Objects.requireNonNull(value, "value");
  }

  public static DocumentId of(String value) {
    return new DocumentId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
