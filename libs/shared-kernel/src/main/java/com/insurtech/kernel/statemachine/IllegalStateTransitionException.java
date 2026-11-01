package com.insurtech.kernel.statemachine;

import com.insurtech.kernel.error.DomainException;
import com.insurtech.kernel.error.ErrorCode;

/** The command is not defined for the current state (CRC-SM-004). Maps to HTTP 409. */
public final class IllegalStateTransitionException extends DomainException {

  private static final long serialVersionUID = 1L;

  public static final ErrorCode CODE = ErrorCode.of("ILLEGAL_STATE_TRANSITION");

  public IllegalStateTransitionException(String machine, String from, String command) {
    super(CODE, machine + ": command " + command + " is not allowed in state " + from);
  }
}
