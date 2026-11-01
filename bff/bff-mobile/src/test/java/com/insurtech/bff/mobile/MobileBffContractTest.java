package com.insurtech.bff.mobile;

import com.insurtech.archtest.ContractRules;
import org.junit.jupiter.api.Test;

/** Fails the build when the BFF reaches into context internals (CRC-ARCH-011). */
class MobileBffContractTest {

  @Test
  void obeysCodeRuleContract() {
    ContractRules.verifyBff("mobile");
  }
}
