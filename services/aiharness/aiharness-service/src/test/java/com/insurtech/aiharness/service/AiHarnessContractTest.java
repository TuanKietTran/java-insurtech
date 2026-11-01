package com.insurtech.aiharness.service;

import com.insurtech.archtest.ContractRules;
import org.junit.jupiter.api.Test;

/** Fails the build when {@code com.insurtech.aiharness} violates the Code Rule Contract. */
class AiHarnessContractTest {

  @Test
  void obeysCodeRuleContract() {
    ContractRules.verifyContext("aiharness");
  }
}
