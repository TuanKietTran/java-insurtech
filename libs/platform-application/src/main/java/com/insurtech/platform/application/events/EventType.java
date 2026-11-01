package com.insurtech.platform.application.events;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Integration event name {@code <context>.<aggregate>.<past-tense-verb>.v<N>}, e.g. {@code
 * claims.claim-line.closed.v1} (CRC-EVT-002).
 */
public record EventType(String value) {

  private static final Pattern FORMAT =
      Pattern.compile("[a-z][a-z0-9]*\\.[a-z][a-z0-9-]*\\.[a-z][a-z0-9-]*\\.v[1-9][0-9]*");

  public EventType {
    Objects.requireNonNull(value, "value");
    if (!FORMAT.matcher(value).matches()) {
      throw new IllegalArgumentException(
          "CRC-EVT-002: event types are <context>.<aggregate>.<past-tense-verb>.v<N>: " + value);
    }
  }

  public String context() {
    return value.substring(0, value.indexOf('.'));
  }

  public int majorVersion() {
    return Integer.parseInt(value.substring(value.lastIndexOf(".v") + 2));
  }

  @Override
  public String toString() {
    return value;
  }
}
