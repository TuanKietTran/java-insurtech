package com.insurtech.regreporting.service;

import com.insurtech.archtest.ContractRules;
import org.junit.jupiter.api.Test;

/** Fails the build when {@code com.insurtech.regreporting} violates the Code Rule Contract. */
class RegReportingContractTest {

  @Test
  void obeysCodeRuleContract() {
    ContractRules.verifyContext("regreporting");
  }
}
