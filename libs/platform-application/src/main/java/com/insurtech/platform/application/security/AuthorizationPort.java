package com.insurtech.platform.application.security;

import com.insurtech.kernel.actor.ActorRef;

/**
 * Policy Decision Point (PRD-F01). Deny by default: handlers call this before loading or mutating
 * data (CRC-SEC-001).
 */
public interface AuthorizationPort {

  AuthorizationDecision decide(ActorRef actor, String action, ResourceRef resource);
}
