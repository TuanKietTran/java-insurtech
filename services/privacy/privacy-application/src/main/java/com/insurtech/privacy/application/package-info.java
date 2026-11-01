/**
 * Use cases of the {@code privacy} bounded context (PRD-R01).
 *
 * <p>Framework-free (CRC-ARCH-013). Each command handler authorizes first ({@code @Authorized},
 * CRC-SEC-001), runs in one {@code TransactionPort} transaction, and persists state, outbox events
 * and the audit record together (CRC-SM-006, CRC-AUD-001), behind {@code IdempotencyPort}
 * (CRC-IDEM-002).
 */
package com.insurtech.privacy.application;
