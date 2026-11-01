package com.insurtech.kernel.money;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;
import net.jqwik.api.constraints.Size;

/** CRC-MON-004 / CRC-TST-003: allocation always sums exactly to the original amount. */
class MoneyAllocationProperties {

  @Property
  void partsSumExactlyToTheWhole(
      @ForAll @LongRange(min = -10_000_000_00L, max = 10_000_000_00L) long cents,
      @ForAll @Size(min = 1, max = 24) List<@IntRange(min = 1, max = 1_000) Integer> weights) {
    Money whole = new Money(BigDecimal.valueOf(cents, 2), CurrencyUnit.of("USD"));
    List<Factor> factors = weights.stream().map(w -> new Factor(BigDecimal.valueOf(w))).toList();

    List<Money> parts = whole.allocate(factors);

    Money sum = parts.stream().reduce(Money.zero(whole.currency()), Money::plus);
    assertThat(sum).isEqualTo(whole);
    assertThat(parts).hasSize(weights.size());
  }

  @Property
  void evenSplitsDifferByAtMostOneMinorUnit(
      @ForAll @LongRange(min = 0, max = 1_000_000_00L) long cents,
      @ForAll @IntRange(min = 1, max = 50) int parts) {
    Money whole = new Money(BigDecimal.valueOf(cents, 2), CurrencyUnit.of("USD"));

    List<Money> split = whole.allocateEvenly(parts);

    BigDecimal max = split.stream().map(Money::amount).reduce(BigDecimal::max).orElseThrow();
    BigDecimal min = split.stream().map(Money::amount).reduce(BigDecimal::min).orElseThrow();
    assertThat(max.subtract(min)).isLessThanOrEqualTo(new BigDecimal("0.01"));
  }
}
