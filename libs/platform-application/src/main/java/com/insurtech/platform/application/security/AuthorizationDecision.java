package com.insurtech.platform.application.security;

import java.util.List;
import java.util.Objects;

/**
 * PDP outcome with the policies that matched and any obligations such as field masks (F01-P0-03).
 */
public record AuthorizationDecision(
    Effect effect, List<String> matchedPolicies, List<String> obligations) {

  /** Allow or deny. */
  public enum Effect {
    ALLOW,
    DENY
  }

  public AuthorizationDecision {
    Objects.requireNonNull(effect, "effect");
    matchedPolicies = List.copyOf(matchedPolicies);
    obligations = List.copyOf(obligations);
  }

  public boolean isAllowed() {
    return effect == Effect.ALLOW;
  }
}
