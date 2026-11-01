package com.insurtech.kernel.actor;

/** Who performed an action; recorded on every audit record (CRC-AUD-002). */
public enum ActorType {
  HUMAN,
  SYSTEM,
  AI_AGENT,
  EXTERNAL
}
