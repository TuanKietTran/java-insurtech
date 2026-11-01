package com.insurtech.vendors.domain;

import java.util.Objects;
import java.util.UUID;

/** Identifier of a {@code Vendor} aggregate (CRC-DOM-001). */
public record VendorId(UUID value) {

  public VendorId {
    Objects.requireNonNull(value, "value");
  }

  public static VendorId of(String value) {
    return new VendorId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
