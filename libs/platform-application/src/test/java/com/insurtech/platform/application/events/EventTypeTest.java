package com.insurtech.platform.application.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EventTypeTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "claims.claim-line.closed.v1",
        "ledger.payment-order.released.v2",
        "policy.term.cancellation-scheduled.v1"
      })
  void acceptsContractNames(String name) {
    assertThat(new EventType(name).value()).isEqualTo(name);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "ClaimClosed",
        "claims.claim.closed",
        "claims.claim.closed.v0",
        "claims.Claim.closed.v1",
        "claims.claim_line.closed.v1"
      })
  void rejectsNonContractNames(String name) {
    assertThatThrownBy(() -> new EventType(name)).hasMessageContaining("CRC-EVT-002");
  }

  @Test
  void exposesContextAndVersion() {
    EventType type = new EventType("claims.claim-line.closed.v3");
    assertThat(type.context()).isEqualTo("claims");
    assertThat(type.majorVersion()).isEqualTo(3);
  }
}
