package com.insurtech.kernel.id;

import java.util.UUID;

/**
 * Source of new identifiers. Injected so domain code stays deterministic (CRC-DOM-010); tests use a
 * fixed sequence.
 */
@FunctionalInterface
public interface IdGenerator {

  UUID next();
}
