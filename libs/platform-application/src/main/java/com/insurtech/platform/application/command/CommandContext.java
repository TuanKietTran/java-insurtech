package com.insurtech.platform.application.command;

import com.insurtech.kernel.actor.ActorRef;
import com.insurtech.kernel.id.CorrelationId;
import com.insurtech.kernel.id.IdempotencyKey;
import com.insurtech.kernel.id.TenantId;
import java.util.Objects;

/**
 * Who is asking, for which tenant, within which flow, and how to deduplicate retries. Carried by
 * every command (CRC-SEC-002, CRC-IDEM-001, CRC-OBS-001).
 */
public record CommandContext(
    TenantId tenantId, ActorRef actor, CorrelationId correlationId, IdempotencyKey idempotencyKey) {

  public CommandContext {
    Objects.requireNonNull(tenantId, "tenantId");
    Objects.requireNonNull(actor, "actor");
    Objects.requireNonNull(correlationId, "correlationId");
    Objects.requireNonNull(idempotencyKey, "idempotencyKey");
  }
}
