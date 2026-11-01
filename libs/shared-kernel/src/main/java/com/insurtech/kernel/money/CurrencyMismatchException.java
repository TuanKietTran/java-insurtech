package com.insurtech.kernel.money;

import com.insurtech.kernel.error.DomainException;
import com.insurtech.kernel.error.ErrorCode;

/** Arithmetic across currencies is never implicit (CRC-MON-002). */
public final class CurrencyMismatchException extends DomainException {

  private static final long serialVersionUID = 1L;

  public CurrencyMismatchException(CurrencyUnit left, CurrencyUnit right) {
    super(ErrorCode.of("CURRENCY_MISMATCH"), "Cannot combine " + left + " with " + right);
  }
}
