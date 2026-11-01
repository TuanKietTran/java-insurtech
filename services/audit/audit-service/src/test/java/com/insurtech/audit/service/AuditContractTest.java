package com.insurtech.audit.service;

import com.insurtech.archtest.ContractRules;
import org.junit.jupiter.api.Test;

/** Fails the build when {@code com.insurtech.audit} violates the Code Rule Contract. */
class AuditContractTest {

  @Test
  void obeysCodeRuleContract() {
    ContractRules.verifyContext("audit");
  }
}
