package com.insurtech.fraud.service;

import com.insurtech.archtest.ContractRules;
import org.junit.jupiter.api.Test;

/** Fails the build when {@code com.insurtech.fraud} violates the Code Rule Contract. */
class FraudContractTest {

  @Test
  void obeysCodeRuleContract() {
    ContractRules.verifyContext("fraud");
  }
}
