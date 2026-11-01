package com.insurtech.kernel.statemachine;

import com.insurtech.kernel.error.ErrorCode;
import java.util.ArrayList;
import java.util.List;

/** Outcome of a transition guard: pass, or fail with reasons (CRC-SM-003). */
public sealed interface GuardResult permits GuardResult.Pass, GuardResult.Fail {

  static GuardResult pass() {
    return Pass.INSTANCE;
  }

  static GuardResult fail(ErrorCode reason, ErrorCode... more) {
    List<ErrorCode> reasons = new ArrayList<>(1 + more.length);
    reasons.add(reason);
    reasons.addAll(List.of(more));
    return new Fail(reasons);
  }

  /** Convenience for guards of the form "condition must hold, otherwise {@code reason}". */
  static GuardResult require(boolean condition, ErrorCode reason) {
    return condition ? pass() : fail(reason);
  }

  /** The guard passed. */
  record Pass() implements GuardResult {
    static final Pass INSTANCE = new Pass();
  }

  /** The guard failed for one or more reasons. */
  record Fail(List<ErrorCode> reasons) implements GuardResult {
    public Fail {
      reasons = List.copyOf(reasons);
      if (reasons.isEmpty()) {
        throw new IllegalArgumentException("A failed guard needs at least one reason");
      }
    }
  }
}
