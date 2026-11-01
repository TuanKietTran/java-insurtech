package com.insurtech.kernel.error;

/** Raised when a value object would be constructed in an invalid state (CRC-DOM-002). */
public final class InvalidValueException extends DomainException {

  private static final long serialVersionUID = 1L;

  public InvalidValueException(ErrorCode code, String message) {
    super(code, message);
  }
}
