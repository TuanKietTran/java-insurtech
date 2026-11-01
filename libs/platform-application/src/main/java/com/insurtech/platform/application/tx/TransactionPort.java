package com.insurtech.platform.application.tx;

import java.util.function.Supplier;

/**
 * Runs work in one database transaction. Keeps application code free of framework annotations
 * (CRC-ARCH-013); state, outbox events and audit records commit together (CRC-SM-006).
 */
public interface TransactionPort {

  <T> T inTransaction(Supplier<T> work);
}
