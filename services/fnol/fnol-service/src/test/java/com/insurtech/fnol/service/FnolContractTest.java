package com.insurtech.fnol.service;

import com.insurtech.archtest.ContractRules;
import org.junit.jupiter.api.Test;

/** Fails the build when {@code com.insurtech.fnol} violates the Code Rule Contract. */
class FnolContractTest {

  @Test
  void obeysCodeRuleContract() {
    ContractRules.verifyContext("fnol");
  }
}
