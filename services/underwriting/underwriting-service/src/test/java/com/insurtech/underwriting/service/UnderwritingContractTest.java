package com.insurtech.underwriting.service;

import com.insurtech.archtest.ContractRules;
import org.junit.jupiter.api.Test;

/** Fails the build when {@code com.insurtech.underwriting} violates the Code Rule Contract. */
class UnderwritingContractTest {

  @Test
  void obeysCodeRuleContract() {
    ContractRules.verifyContext("underwriting");
  }
}
