package com.insurtech.rating.service;

import com.insurtech.archtest.ContractRules;
import org.junit.jupiter.api.Test;

/** Fails the build when {@code com.insurtech.rating} violates the Code Rule Contract. */
class RatingContractTest {

  @Test
  void obeysCodeRuleContract() {
    ContractRules.verifyContext("rating");
  }
}
