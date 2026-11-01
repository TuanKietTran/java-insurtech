package com.insurtech.kernel.money;

import com.insurtech.kernel.error.ErrorCode;
import com.insurtech.kernel.error.InvalidValueException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Monetary amount in one currency (CRC-MON-001). Amounts always sit at the currency's minor-unit
 * scale; anything finer must be rounded explicitly by the caller with a named {@link RoundingMode}
 * (CRC-MON-003). Splitting uses largest-remainder allocation so the parts sum exactly to the whole
 * (CRC-MON-004).
 */
public record Money(BigDecimal amount, CurrencyUnit currency) {

  static final ErrorCode EXCESS_PRECISION = ErrorCode.of("MONEY_EXCESS_PRECISION");
  static final ErrorCode INVALID_ALLOCATION = ErrorCode.of("MONEY_INVALID_ALLOCATION");

  public Money {
    Objects.requireNonNull(amount, "amount");
    Objects.requireNonNull(currency, "currency");
    try {
      amount = amount.setScale(currency.minorUnits(), RoundingMode.UNNECESSARY);
    } catch (ArithmeticException e) {
      throw new InvalidValueException(
          EXCESS_PRECISION,
          amount + " has more precision than " + currency + " allows; round explicitly");
    }
  }

  public static Money of(String amount, String currency) {
    return new Money(new BigDecimal(amount), CurrencyUnit.of(currency));
  }

  public static Money zero(CurrencyUnit currency) {
    return new Money(BigDecimal.ZERO, currency);
  }

  /** Rounds {@code amount} to the currency's minor units with an explicit mode (CRC-MON-003). */
  public static Money rounded(BigDecimal amount, CurrencyUnit currency, RoundingMode mode) {
    return new Money(amount.setScale(currency.minorUnits(), mode), currency);
  }

  public Money plus(Money other) {
    requireSameCurrency(other);
    return new Money(amount.add(other.amount), currency);
  }

  public Money minus(Money other) {
    requireSameCurrency(other);
    return new Money(amount.subtract(other.amount), currency);
  }

  public Money negate() {
    return new Money(amount.negate(), currency);
  }

  public Money times(Factor factor, RoundingMode mode) {
    return rounded(amount.multiply(factor.value()), currency, mode);
  }

  public boolean isZero() {
    return amount.signum() == 0;
  }

  public boolean isNegative() {
    return amount.signum() < 0;
  }

  public boolean isPositive() {
    return amount.signum() > 0;
  }

  public boolean isGreaterThan(Money other) {
    requireSameCurrency(other);
    return amount.compareTo(other.amount) > 0;
  }

  /** Splits into {@code parts} near-equal amounts that sum exactly to this amount. */
  public List<Money> allocateEvenly(int parts) {
    if (parts < 1) {
      throw new InvalidValueException(INVALID_ALLOCATION, "parts must be >= 1: " + parts);
    }
    return allocate(Collections.nCopies(parts, Factor.ONE));
  }

  /**
   * Splits proportionally to {@code weights} using exact integer arithmetic on minor units; the
   * leftover minor units go to the largest remainders (ties to the earliest weight).
   */
  public List<Money> allocate(List<Factor> weights) {
    if (weights.isEmpty()) {
      throw new InvalidValueException(INVALID_ALLOCATION, "At least one weight is required");
    }
    int scale = weights.stream().mapToInt(w -> Math.max(w.value().scale(), 0)).max().orElse(0);
    List<BigInteger> integerWeights =
        weights.stream().map(w -> w.value().movePointRight(scale).toBigIntegerExact()).toList();
    BigInteger total = integerWeights.stream().reduce(BigInteger.ZERO, BigInteger::add);
    if (total.signum() == 0) {
      throw new InvalidValueException(INVALID_ALLOCATION, "Weights must not all be zero");
    }

    int minorUnits = currency.minorUnits();
    BigInteger units = amount.movePointRight(minorUnits).toBigIntegerExact();
    BigInteger magnitude = units.abs();

    List<BigInteger> shares = new ArrayList<>(weights.size());
    List<BigInteger> remainders = new ArrayList<>(weights.size());
    BigInteger distributed = BigInteger.ZERO;
    for (BigInteger weight : integerWeights) {
      BigInteger[] divided = magnitude.multiply(weight).divideAndRemainder(total);
      shares.add(divided[0]);
      remainders.add(divided[1]);
      distributed = distributed.add(divided[0]);
    }

    int leftover = magnitude.subtract(distributed).intValueExact();
    IntStream.range(0, weights.size())
        .boxed()
        .sorted(Comparator.comparing(remainders::get).reversed())
        .limit(leftover)
        .forEach(i -> shares.set(i, shares.get(i).add(BigInteger.ONE)));

    int sign = units.signum() < 0 ? -1 : 1;
    return shares.stream()
        .map(share -> new BigDecimal(sign < 0 ? share.negate() : share, minorUnits))
        .map(value -> new Money(value, currency))
        .toList();
  }

  private void requireSameCurrency(Money other) {
    if (!currency.equals(other.currency)) {
      throw new CurrencyMismatchException(currency, other.currency);
    }
  }

  @Override
  public String toString() {
    return amount.toPlainString() + " " + currency;
  }
}
