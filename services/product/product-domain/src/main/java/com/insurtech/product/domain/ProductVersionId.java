package com.insurtech.product.domain;

import java.util.Objects;
import java.util.UUID;

/** Identifier of a {@code ProductVersion} aggregate (CRC-DOM-001). */
public record ProductVersionId(UUID value) {

  public ProductVersionId {
    Objects.requireNonNull(value, "value");
  }

  public static ProductVersionId of(String value) {
    return new ProductVersionId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
