package com.insurtech.kernel.money;

import com.insurtech.kernel.error.ErrorCode;
import com.insurtech.kernel.error.InvalidValueException;
import java.math.BigDecimal;
import java.util.Objects;

/** Non-negative multiplicative factor, e.g. a rating factor of 1.25 (CRC-MON-006). */
public record Factor(BigDecimal value) {

  public static final Factor ONE = new Factor(BigDecimal.ONE);

  public Factor {
    Objects.requireNonNull(value, "value");
    if (value.signum() < 0) {
      throw new InvalidValueException(ErrorCode.of("NEGATIVE_FACTOR"), "Factor < 0: " + value);
    }
  }

  public static Factor of(String value) {
    return new Factor(new BigDecimal(value));
  }
}
