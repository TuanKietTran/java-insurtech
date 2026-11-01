package com.insurtech.notifications.service;

import com.insurtech.archtest.ContractRules;
import org.junit.jupiter.api.Test;

/** Fails the build when {@code com.insurtech.notifications} violates the Code Rule Contract. */
class NotificationsContractTest {

  @Test
  void obeysCodeRuleContract() {
    ContractRules.verifyContext("notifications");
  }
}
