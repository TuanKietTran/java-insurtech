package com.insurtech.documents.service;

import com.insurtech.archtest.ContractRules;
import org.junit.jupiter.api.Test;

/** Fails the build when {@code com.insurtech.documents} violates the Code Rule Contract. */
class DocumentsContractTest {

  @Test
  void obeysCodeRuleContract() {
    ContractRules.verifyContext("documents");
  }
}
