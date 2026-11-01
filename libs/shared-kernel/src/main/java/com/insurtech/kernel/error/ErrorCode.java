package com.insurtech.kernel.error;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Stable, machine-readable error code such as {@code CLAIM_LINE_HAS_OUTSTANDING_RESERVE}
 * (CRC-DOM-009). Codes are documented in each context's {@code -api} module (CRC-ERR-001).
 */
public record ErrorCode(String value) {

  private static final Pattern FORMAT = Pattern.compile("[A-Z][A-Z0-9]*(_[A-Z0-9]+)*");

  public ErrorCode {
    Objects.requireNonNull(value, "value");
    if (!FORMAT.matcher(value).matches()) {
      throw new IllegalArgumentException("Error codes are UPPER_SNAKE_CASE: " + value);
    }
  }

  public static ErrorCode of(String value) {
    return new ErrorCode(value);
  }

  @Override
  public String toString() {
    return value;
  }
}
