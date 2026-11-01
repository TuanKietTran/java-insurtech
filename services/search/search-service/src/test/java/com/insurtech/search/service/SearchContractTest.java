package com.insurtech.search.service;

import com.insurtech.archtest.ContractRules;
import org.junit.jupiter.api.Test;

/** Fails the build when {@code com.insurtech.search} violates the Code Rule Contract. */
class SearchContractTest {

  @Test
  void obeysCodeRuleContract() {
    ContractRules.verifyContext("search");
  }
}
