package com.insurtech.underwriting.domain;

import java.util.Objects;
import java.util.UUID;

/** Identifier of a {@code Submission} aggregate (CRC-DOM-001). */
public record SubmissionId(UUID value) {

  public SubmissionId {
    Objects.requireNonNull(value, "value");
  }

  public static SubmissionId of(String value) {
    return new SubmissionId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
