package com.insurtech.kernel.time;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Business time with an explicit zone and precision, e.g. a loss reported as "sometime on 3 March"
 * versus "at 14:05" (CRC-TIME-006). Coverage evaluation uses {@link #window()} to detect ambiguity.
 */
public record BusinessDateTime(ZonedDateTime value, Precision precision) {

  /** How precisely the business time is known. */
  public enum Precision {
    DATE_ONLY,
    APPROXIMATE,
    EXACT
  }

  private static final Duration EXACT_WINDOW = Duration.ofMillis(1);

  public BusinessDateTime {
    Objects.requireNonNull(value, "value");
    Objects.requireNonNull(precision, "precision");
    if (precision == Precision.DATE_ONLY) {
      value = value.toLocalDate().atStartOfDay(value.getZone());
    }
  }

  public static BusinessDateTime ofDate(LocalDate date, ZoneId zone) {
    return new BusinessDateTime(date.atStartOfDay(zone), Precision.DATE_ONLY);
  }

  public static BusinessDateTime exact(ZonedDateTime value) {
    return new BusinessDateTime(value, Precision.EXACT);
  }

  /** The interval of instants this business time could denote. */
  public EffectivePeriod window() {
    return switch (precision) {
      case DATE_ONLY, APPROXIMATE -> {
        ZonedDateTime startOfDay = value.toLocalDate().atStartOfDay(value.getZone());
        yield new EffectivePeriod(startOfDay.toInstant(), startOfDay.plusDays(1).toInstant());
      }
      case EXACT -> new EffectivePeriod(value.toInstant(), value.toInstant().plus(EXACT_WINDOW));
    };
  }
}
