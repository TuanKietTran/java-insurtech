package com.insurtech.archtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

/** Proves each rule actually catches what it claims to, using deliberately violating fixtures. */
class ContractRulesTest {

  private static final JavaClasses VIOLATING =
      new ClassFileImporter().importPackages("com.insurtech.fixture.violating");

  private static final JavaClasses CLEAN =
      new ClassFileImporter().importPackages("com.insurtech.fixture.clean");

  @Test
  void detectsPublicSettersInDomain() {
    assertViolated(ContractRules.CRC_DOM_003_NO_PUBLIC_SETTERS);
  }

  @Test
  void detectsBareAgent() {
    assertViolated(ContractRules.CRC_DOM_011_NO_BARE_AGENT);
  }

  @Test
  void detectsFloatingPointMoney() {
    assertViolated(ContractRules.CRC_MON_001_NO_FLOATING_POINT_FIELDS);
    assertViolated(ContractRules.CRC_MON_001_NO_FLOATING_POINT_RESULTS);
  }

  @Test
  void detectsSystemClock() {
    assertViolated(ContractRules.CRC_TIME_002_INJECTED_CLOCK);
  }

  @Test
  void detectsUnauthorizedHandlers() {
    assertViolated(ContractRules.CRC_SEC_001_HANDLERS_ARE_AUTHORIZED);
  }

  @Test
  void verifyReportsAllViolationsTogether() {
    assertThatThrownBy(() -> ContractRules.verify(VIOLATING, "fixture"))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("CRC-DOM-003")
        .hasMessageContaining("CRC-TIME-002")
        .hasMessageContaining("CRC-SEC-001");
  }

  @Test
  void compliantCodePasses() {
    ContractRules.verify(CLEAN, "fixture");
  }

  private static void assertViolated(ArchRule rule) {
    assertThat(rule.evaluate(VIOLATING).hasViolation()).as(rule.getDescription()).isTrue();
  }
}
