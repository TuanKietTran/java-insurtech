package com.insurtech.bff.vendor;

import com.insurtech.archtest.ContractRules;
import org.junit.jupiter.api.Test;

/** Fails the build when the BFF reaches into context internals (CRC-ARCH-011). */
class VendorBffContractTest {

  @Test
  void obeysCodeRuleContract() {
    ContractRules.verifyBff("vendor");
  }
}
