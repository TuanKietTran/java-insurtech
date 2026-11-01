package com.insurtech.product.service;

import com.insurtech.archtest.ContractRules;
import org.junit.jupiter.api.Test;

/** Fails the build when {@code com.insurtech.product} violates the Code Rule Contract. */
class ProductContractTest {

  @Test
  void obeysCodeRuleContract() {
    ContractRules.verifyContext("product");
  }
}
