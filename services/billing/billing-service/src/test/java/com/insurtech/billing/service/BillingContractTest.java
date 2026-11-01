package com.insurtech.billing.service;

import com.insurtech.archtest.ContractRules;
import org.junit.jupiter.api.Test;

/** Fails the build when {@code com.insurtech.billing} violates the Code Rule Contract. */
class BillingContractTest {

  @Test
  void obeysCodeRuleContract() {
    ContractRules.verifyContext("billing");
  }
}
