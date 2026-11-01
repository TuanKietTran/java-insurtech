package com.insurtech.platform.application.audit;

/**
 * Sink for audit records. Implementations write through the outbox inside the caller's transaction
 * so a state change can never commit without its audit record (CRC-AUD-001, CRC-AUD-006).
 */
public interface AuditPort {

  void record(AuditRecord record);
}
