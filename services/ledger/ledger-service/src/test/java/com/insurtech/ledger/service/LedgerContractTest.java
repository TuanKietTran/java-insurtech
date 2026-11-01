package com.insurtech.ledger.service;

import com.insurtech.archtest.ContractRules;
import org.junit.jupiter.api.Test;

/** Fails the build when {@code com.insurtech.ledger} violates the Code Rule Contract. */
class LedgerContractTest {

  @Test
  void obeysCodeRuleContract() {
    ContractRules.verifyContext("ledger");
  }
}
