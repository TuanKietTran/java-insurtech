package com.insurtech.privacy.service;

import com.insurtech.archtest.ContractRules;
import org.junit.jupiter.api.Test;

/** Fails the build when {@code com.insurtech.privacy} violates the Code Rule Contract. */
class PrivacyContractTest {

  @Test
  void obeysCodeRuleContract() {
    ContractRules.verifyContext("privacy");
  }
}
