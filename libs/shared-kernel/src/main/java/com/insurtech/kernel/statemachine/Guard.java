package com.insurtech.kernel.statemachine;

/**
 * Side-effect-free precondition of a transition (CRC-SM-003).
 *
 * @param <X> the subject the guard inspects, usually the aggregate
 * @param <C> the command type of the state machine
 */
@FunctionalInterface
public interface Guard<X, C> {

  GuardResult check(X subject, C command);
}
