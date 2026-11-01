package com.insurtech.ledger.domain;

import java.util.Objects;
import java.util.UUID;

/** Identifier of a {@code JournalEntry} aggregate (CRC-DOM-001). */
public record JournalEntryId(UUID value) {

  public JournalEntryId {
    Objects.requireNonNull(value, "value");
  }

  public static JournalEntryId of(String value) {
    return new JournalEntryId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
