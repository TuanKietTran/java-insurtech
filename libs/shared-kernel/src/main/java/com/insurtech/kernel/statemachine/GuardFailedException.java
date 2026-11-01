package com.insurtech.kernel.statemachine;

import com.insurtech.kernel.error.DomainException;
import com.insurtech.kernel.error.ErrorCode;
import java.util.List;

/** The transition exists but one or more guards failed; carries every reason (CRC-SM-003). */
public final class GuardFailedException extends DomainException {

  private static final long serialVersionUID = 1L;

  public static final ErrorCode CODE = ErrorCode.of("TRANSITION_GUARD_FAILED");

  @SuppressWarnings("serial") // ErrorCode is an immutable record; the list is an immutable copy.
  private final List<ErrorCode> reasons;

  public GuardFailedException(
      String machine, String from, String command, List<ErrorCode> reasons) {
    super(CODE, machine + ": " + command + " from " + from + " blocked by " + reasons);
    this.reasons = List.copyOf(reasons);
  }

  public List<ErrorCode> reasons() {
    return reasons;
  }
}
