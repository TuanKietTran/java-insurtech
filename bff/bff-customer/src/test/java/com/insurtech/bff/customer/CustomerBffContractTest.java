package com.insurtech.bff.customer;

import com.insurtech.archtest.ContractRules;
import org.junit.jupiter.api.Test;

/** Fails the build when the BFF reaches into context internals (CRC-ARCH-011). */
class CustomerBffContractTest {

  @Test
  void obeysCodeRuleContract() {
    ContractRules.verifyBff("customer");
  }
}
