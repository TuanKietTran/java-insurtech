package com.insurtech.rules.service;

import com.insurtech.archtest.ContractRules;
import org.junit.jupiter.api.Test;

/** Fails the build when {@code com.insurtech.rules} violates the Code Rule Contract. */
class RulesContractTest {

  @Test
  void obeysCodeRuleContract() {
    ContractRules.verifyContext("rules");
  }
}
