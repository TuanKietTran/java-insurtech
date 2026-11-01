package com.insurtech.kernel.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.insurtech.kernel.error.InvalidValueException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.junit.jupiter.api.Test;

class MoneyTest {

  @Test
  void normalizesToCurrencyScale() {
    assertThat(Money.of("10", "USD").amount()).isEqualTo(new BigDecimal("10.00"));
    assertThat(Money.of("10", "JPY").amount()).isEqualTo(new BigDecimal("10"));
  }

  @Test
  void rejectsExcessPrecisionInsteadOfRoundingSilently() {
    assertThatThrownBy(() -> Money.of("10.005", "USD"))
        .isInstanceOf(InvalidValueException.class)
        .hasMessageContaining("round explicitly");
  }

  @Test
  void roundsOnlyWithAnExplicitMode() {
    CurrencyUnit usd = CurrencyUnit.of("USD");
    assertThat(Money.rounded(new BigDecimal("10.005"), usd, RoundingMode.HALF_EVEN))
        .isEqualTo(Money.of("10.00", "USD"));
    assertThat(Money.rounded(new BigDecimal("10.015"), usd, RoundingMode.HALF_EVEN))
        .isEqualTo(Money.of("10.02", "USD"));
  }

  @Test
  void refusesToMixCurrencies() {
    assertThatThrownBy(() -> Money.of("1", "USD").plus(Money.of("1", "EUR")))
        .isInstanceOf(CurrencyMismatchException.class);
  }

  @Test
  void allocatesLeftoverCentsToLargestRemainders() {
    List<Money> parts = Money.of("100.00", "USD").allocateEvenly(3);
    assertThat(parts)
        .containsExactly(
            Money.of("33.34", "USD"), Money.of("33.33", "USD"), Money.of("33.33", "USD"));
  }

  @Test
  void allocatesProportionallyToWeights() {
    List<Money> parts =
        Money.of("1000.00", "USD").allocate(List.of(Factor.of("0.7"), Factor.of("0.3")));
    assertThat(parts).containsExactly(Money.of("700.00", "USD"), Money.of("300.00", "USD"));
  }

  @Test
  void allocatesNegativeAmounts() {
    List<Money> parts = Money.of("-0.05", "USD").allocateEvenly(2);
    assertThat(parts).containsExactly(Money.of("-0.03", "USD"), Money.of("-0.02", "USD"));
  }

  @Test
  void multipliesByFactorWithExplicitRounding() {
    assertThat(Money.of("100.00", "USD").times(Factor.of("1.333"), RoundingMode.HALF_EVEN))
        .isEqualTo(Money.of("133.30", "USD"));
  }

  @Test
  void percentageConvertsExactlyToFactor() {
    assertThat(Percentage.of("12.5").asFactor()).isEqualTo(new Factor(new BigDecimal("0.125")));
  }
}
