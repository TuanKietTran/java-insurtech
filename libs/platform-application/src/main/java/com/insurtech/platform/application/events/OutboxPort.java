package com.insurtech.platform.application.events;

import java.util.List;

/**
 * Transactional outbox (CRC-EVT-001). Appends within the caller's transaction; a relay publishes
 * after commit. Business code never talks to the broker directly.
 */
public interface OutboxPort {

  void append(List<EventEnvelope<?>> events);
}
