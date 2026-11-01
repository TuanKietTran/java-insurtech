package com.insurtech.identity.service;

import com.insurtech.archtest.ContractRules;
import org.junit.jupiter.api.Test;

/** Fails the build when {@code com.insurtech.identity} violates the Code Rule Contract. */
class IdentityContractTest {

  @Test
  void obeysCodeRuleContract() {
    ContractRules.verifyContext("identity");
  }
}
