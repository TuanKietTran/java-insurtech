/**
 * Use cases of the {@code claims} bounded context (PRD-C02, PRD-C03, PRD-C04, PRD-C05).
 *
 * <p>Framework-free (CRC-ARCH-013). Each command handler authorizes first ({@code @Authorized},
 * CRC-SEC-001), runs in one {@code TransactionPort} transaction, and persists state, outbox events
 * and the audit record together (CRC-SM-006, CRC-AUD-001), behind {@code IdempotencyPort}
 * (CRC-IDEM-002).
 */
package com.insurtech.claims.application;
