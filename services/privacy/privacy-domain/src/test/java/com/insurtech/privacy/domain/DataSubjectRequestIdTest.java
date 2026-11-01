package com.insurtech.privacy.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class DataSubjectRequestIdTest {

  @Test
  void roundTripsThroughItsStringForm() {
    UUID raw = UUID.fromString("7d444840-9dc0-11d1-b245-5ffdce74fad2");

    assertThat(DataSubjectRequestId.of(raw.toString())).isEqualTo(new DataSubjectRequestId(raw));
  }
}
