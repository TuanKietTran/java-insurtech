package com.insurtech.kernel.id;

import com.insurtech.kernel.error.ErrorCode;
import com.insurtech.kernel.error.InvalidValueException;
import java.util.Objects;
import java.util.regex.Pattern;

/** Client- or cause-derived key that makes a mutating operation safe to retry (CRC-IDEM-*). */
public record IdempotencyKey(String value) {

  private static final Pattern FORMAT = Pattern.compile("[A-Za-z0-9_\\-:.]{8,128}");

  public IdempotencyKey {
    Objects.requireNonNull(value, "value");
    if (!FORMAT.matcher(value).matches()) {
      throw new InvalidValueException(
          ErrorCode.of("INVALID_IDEMPOTENCY_KEY"),
          "Idempotency keys are 8-128 characters of [A-Za-z0-9_-:.]");
    }
  }

  @Override
  public String toString() {
    return value;
  }
}
