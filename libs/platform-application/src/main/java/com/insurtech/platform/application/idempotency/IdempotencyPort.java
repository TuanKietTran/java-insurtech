package com.insurtech.platform.application.idempotency;

import com.insurtech.kernel.id.IdempotencyKey;
import java.util.function.Supplier;

/**
 * Executes a mutating operation at most once per key (CRC-IDEM-002). Same key and same request hash
 * replays the stored result; same key with a different hash is rejected. Financial operations keep
 * their keys permanently (CRC-IDEM-003).
 */
public interface IdempotencyPort {

  <T> T execute(IdempotencyKey key, String operation, String requestHash, Supplier<T> work);
}
