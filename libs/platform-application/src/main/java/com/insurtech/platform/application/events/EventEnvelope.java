package com.insurtech.platform.application.events;

import com.insurtech.kernel.actor.ActorRef;
import com.insurtech.kernel.id.CorrelationId;
import com.insurtech.kernel.id.TenantId;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Mandatory envelope for every integration event (CRC-EVT-003, PRD-F03). Partitioned by {@code
 * aggregateId}; consumers use {@code aggregateVersion} to detect gaps (CRC-EVT-006).
 *
 * @param <P> payload type; carries IDs and business facts, sensitive data by reference only
 *     (CRC-EVT-007)
 */
public record EventEnvelope<P>(
    UUID eventId,
    EventType eventType,
    int schemaVersion,
    TenantId tenantId,
    String aggregateType,
    String aggregateId,
    long aggregateVersion,
    Instant occurredAt,
    @Nullable Instant effectiveAt,
    CorrelationId correlationId,
    @Nullable UUID causationId,
    ActorRef actor,
    @Nullable String traceparent,
    P payload) {

  public EventEnvelope {
    Objects.requireNonNull(eventId, "eventId");
    Objects.requireNonNull(eventType, "eventType");
    Objects.requireNonNull(tenantId, "tenantId");
    Objects.requireNonNull(aggregateType, "aggregateType");
    Objects.requireNonNull(aggregateId, "aggregateId");
    Objects.requireNonNull(occurredAt, "occurredAt");
    Objects.requireNonNull(correlationId, "correlationId");
    Objects.requireNonNull(actor, "actor");
    Objects.requireNonNull(payload, "payload");
    if (schemaVersion != eventType.majorVersion()) {
      throw new IllegalArgumentException(
          "schemaVersion " + schemaVersion + " does not match " + eventType);
    }
  }
}
