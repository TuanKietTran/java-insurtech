package com.insurtech.kernel.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class BusinessDateTimeTest {

  private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");

  @Test
  void dateOnlyLossCoversTheWholeLocalDay() {
    BusinessDateTime loss = BusinessDateTime.ofDate(LocalDate.of(2026, 3, 3), NEW_YORK);

    EffectivePeriod window = loss.window();

    assertThat(window.start()).isEqualTo(Instant.parse("2026-03-03T05:00:00Z"));
    assertThat(window.end()).isEqualTo(Instant.parse("2026-03-04T05:00:00Z"));
  }

  @Test
  void dstShortDayIsTwentyThreeHours() {
    BusinessDateTime loss = BusinessDateTime.ofDate(LocalDate.of(2026, 3, 8), NEW_YORK);

    EffectivePeriod window = loss.window();

    assertThat(window.end().getEpochSecond() - window.start().getEpochSecond())
        .isEqualTo(23 * 3600);
  }

  @Test
  void periodsAreHalfOpen() {
    EffectivePeriod term =
        new EffectivePeriod(
            Instant.parse("2026-01-01T05:01:00Z"), Instant.parse("2027-01-01T05:01:00Z"));

    assertThat(term.contains(Instant.parse("2026-01-01T05:01:00Z"))).isTrue();
    assertThat(term.contains(Instant.parse("2027-01-01T05:01:00Z"))).isFalse();
  }
}
