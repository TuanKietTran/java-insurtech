package com.insurtech.policy.service;

import com.insurtech.archtest.ContractRules;
import org.junit.jupiter.api.Test;

/** Fails the build when {@code com.insurtech.policy} violates the Code Rule Contract. */
class PolicyContractTest {

  @Test
  void obeysCodeRuleContract() {
    ContractRules.verifyContext("policy");
  }
}
