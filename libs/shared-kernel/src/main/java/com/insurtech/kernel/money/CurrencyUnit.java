package com.insurtech.kernel.money;

import com.insurtech.kernel.error.ErrorCode;
import com.insurtech.kernel.error.InvalidValueException;
import java.util.Currency;
import java.util.Objects;

/** ISO 4217 currency with its minor-unit scale (CRC-MON-001). */
public record CurrencyUnit(String code) {

  static final ErrorCode UNKNOWN_CURRENCY = ErrorCode.of("UNKNOWN_CURRENCY");

  public CurrencyUnit {
    Objects.requireNonNull(code, "code");
    if (!code.matches("[A-Z]{3}")) {
      throw new InvalidValueException(UNKNOWN_CURRENCY, "Not an ISO 4217 code: " + code);
    }
    Currency currency = lookup(code);
    if (currency.getDefaultFractionDigits() < 0) {
      throw new InvalidValueException(UNKNOWN_CURRENCY, "Currency has no minor unit: " + code);
    }
  }

  public static CurrencyUnit of(String code) {
    return new CurrencyUnit(code);
  }

  /** Number of digits after the decimal point, e.g. 2 for USD, 0 for JPY. */
  public int minorUnits() {
    return lookup(code).getDefaultFractionDigits();
  }

  private static Currency lookup(String code) {
    try {
      return Currency.getInstance(code);
    } catch (IllegalArgumentException e) {
      throw new InvalidValueException(UNKNOWN_CURRENCY, "Unknown ISO 4217 currency: " + code);
    }
  }

  @Override
  public String toString() {
    return code;
  }
}
