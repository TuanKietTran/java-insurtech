package com.insurtech.commissions.service;

import com.insurtech.archtest.ContractRules;
import org.junit.jupiter.api.Test;

/** Fails the build when {@code com.insurtech.commissions} violates the Code Rule Contract. */
class CommissionsContractTest {

  @Test
  void obeysCodeRuleContract() {
    ContractRules.verifyContext("commissions");
  }
}
