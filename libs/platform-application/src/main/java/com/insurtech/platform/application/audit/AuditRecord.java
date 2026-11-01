package com.insurtech.platform.application.audit;

import com.insurtech.kernel.actor.ActorRef;
import com.insurtech.kernel.id.CorrelationId;
import com.insurtech.kernel.id.IdempotencyKey;
import com.insurtech.kernel.id.TenantId;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Immutable record of one state-changing command (CRC-AUD-002, PRD-F02). Written in the same
 * transaction as the change it describes (CRC-AUD-001).
 *
 * @param changeSet business-field diff; sensitive values must already be tokenized (PRD-R01)
 * @param reasonCode configurable reason taxonomy code, when the command requires one
 * @param authorityOutcome result of the authority-limit check for financial commands
 * @param effectiveAt business effective time, when it differs from recordedAt
 */
public record AuditRecord(
    UUID auditId,
    TenantId tenantId,
    ActorRef actor,
    String commandType,
    AggregateRef target,
    Map<String, String> changeSet,
    @Nullable String reasonCode,
    @Nullable String authorityOutcome,
    CorrelationId correlationId,
    @Nullable UUID causationId,
    IdempotencyKey idempotencyKey,
    String channel,
    Instant recordedAt,
    @Nullable Instant effectiveAt) {

  public AuditRecord {
    Objects.requireNonNull(auditId, "auditId");
    Objects.requireNonNull(tenantId, "tenantId");
    Objects.requireNonNull(actor, "actor");
    Objects.requireNonNull(commandType, "commandType");
    Objects.requireNonNull(target, "target");
    changeSet = Map.copyOf(changeSet);
    Objects.requireNonNull(correlationId, "correlationId");
    Objects.requireNonNull(idempotencyKey, "idempotencyKey");
    Objects.requireNonNull(channel, "channel");
    Objects.requireNonNull(recordedAt, "recordedAt");
  }
}
