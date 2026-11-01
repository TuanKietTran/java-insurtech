package com.insurtech.kernel.error;

/**
 * Base type for business rule violations (CRC-DOM-009). Adapters map subclasses to RFC 9457 Problem
 * Details using {@link #code()} (CRC-ERR-001, CRC-API-003).
 */
public abstract class DomainException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final ErrorCode code;

  protected DomainException(ErrorCode code, String message) {
    super(message);
    this.code = code;
  }

  public ErrorCode code() {
    return code;
  }
}
