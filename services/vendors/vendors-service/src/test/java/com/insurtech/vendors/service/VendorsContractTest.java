package com.insurtech.vendors.service;

import com.insurtech.archtest.ContractRules;
import org.junit.jupiter.api.Test;

/** Fails the build when {@code com.insurtech.vendors} violates the Code Rule Contract. */
class VendorsContractTest {

  @Test
  void obeysCodeRuleContract() {
    ContractRules.verifyContext("vendors");
  }
}
