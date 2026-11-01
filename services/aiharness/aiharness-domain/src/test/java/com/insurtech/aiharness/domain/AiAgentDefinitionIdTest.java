package com.insurtech.aiharness.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class AiAgentDefinitionIdTest {

  @Test
  void roundTripsThroughItsStringForm() {
    UUID raw = UUID.fromString("7d444840-9dc0-11d1-b245-5ffdce74fad2");

    assertThat(AiAgentDefinitionId.of(raw.toString())).isEqualTo(new AiAgentDefinitionId(raw));
  }
}
