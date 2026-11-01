package com.insurtech.claims.service;

import com.insurtech.archtest.ContractRules;
import org.junit.jupiter.api.Test;

/** Fails the build when {@code com.insurtech.claims} violates the Code Rule Contract. */
class ClaimsContractTest {

  @Test
  void obeysCodeRuleContract() {
    ContractRules.verifyContext("claims");
  }
}
