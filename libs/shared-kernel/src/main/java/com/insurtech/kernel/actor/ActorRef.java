package com.insurtech.kernel.actor;

import com.insurtech.kernel.id.PrincipalId;
import java.util.List;
import java.util.Objects;

/**
 * The acting principal plus its on-behalf-of chain, outermost delegator first. For example an AI
 * Agent acting for an adjuster is {@code (AI_AGENT, agentPrincipal, [adjuster])} (CRC-AUD-002,
 * CRC-AI-009).
 */
public record ActorRef(ActorType type, PrincipalId principal, List<PrincipalId> onBehalfOf) {

  public ActorRef {
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(principal, "principal");
    onBehalfOf = List.copyOf(onBehalfOf);
  }

  public static ActorRef human(PrincipalId principal) {
    return new ActorRef(ActorType.HUMAN, principal, List.of());
  }

  public static ActorRef system(PrincipalId servicePrincipal) {
    return new ActorRef(ActorType.SYSTEM, servicePrincipal, List.of());
  }

  public static ActorRef aiAgent(PrincipalId aiAgentPrincipal, PrincipalId delegator) {
    return new ActorRef(ActorType.AI_AGENT, aiAgentPrincipal, List.of(delegator));
  }
}
