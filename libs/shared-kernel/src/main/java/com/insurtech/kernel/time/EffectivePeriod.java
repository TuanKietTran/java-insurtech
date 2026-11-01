package com.insurtech.kernel.time;

import com.insurtech.kernel.error.ErrorCode;
import com.insurtech.kernel.error.InvalidValueException;
import java.time.Instant;
import java.util.Objects;

/**
 * Half-open business validity interval {@code [start, end)} (CRC-TIME-004). Open-ended periods use
 * {@link Instant#MAX} as the end.
 */
public record EffectivePeriod(Instant start, Instant end) {

  public EffectivePeriod {
    Objects.requireNonNull(start, "start");
    Objects.requireNonNull(end, "end");
    if (!start.isBefore(end)) {
      throw new InvalidValueException(
          ErrorCode.of("INVALID_EFFECTIVE_PERIOD"),
          "start must be before end: " + start + ", " + end);
    }
  }

  public static EffectivePeriod from(Instant start) {
    return new EffectivePeriod(start, Instant.MAX);
  }

  public boolean contains(Instant instant) {
    return !instant.isBefore(start) && instant.isBefore(end);
  }

  public boolean overlaps(EffectivePeriod other) {
    return start.isBefore(other.end) && other.start.isBefore(end);
  }

  public boolean isOpenEnded() {
    return end.equals(Instant.MAX);
  }
}
