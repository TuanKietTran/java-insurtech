package com.insurtech.kernel.money;

import com.insurtech.kernel.error.ErrorCode;
import com.insurtech.kernel.error.InvalidValueException;
import java.math.BigDecimal;
import java.util.Objects;

/** Non-negative percentage expressed in points: {@code 12.5} means 12.5% (CRC-MON-006). */
public record Percentage(BigDecimal points) {

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  public Percentage {
    Objects.requireNonNull(points, "points");
    if (points.signum() < 0) {
      throw new InvalidValueException(
          ErrorCode.of("NEGATIVE_PERCENTAGE"), "Percentage < 0: " + points);
    }
  }

  public static Percentage of(String points) {
    return new Percentage(new BigDecimal(points));
  }

  /** The equivalent factor, e.g. 12.5% → 0.125. Exact: division by 100 only shifts the scale. */
  public Factor asFactor() {
    return new Factor(points.divide(HUNDRED));
  }
}
