package com.insurtech.archtest;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.library.GeneralCodingRules;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * The automatable part of {@code spec/CODE-RULE-CONTRACT.md}, one constant per rule ID. Services
 * call {@link #verifyContext(String)} and BFFs call {@link #verifyBff(String)} from a unit test, so
 * a violation fails {@code ./gradlew build}.
 */
public final class ContractRules {

  private static final String ROOT = "com.insurtech";

  private static final String[] PURE_PACKAGES = {
    "com.insurtech..domain..", "com.insurtech.kernel.."
  };

  private static final String[] FRAMEWORK_PACKAGES = {
    "org.springframework..",
    "jakarta.persistence..",
    "jakarta.servlet..",
    "jakarta.transaction..",
    "org.hibernate..",
    "org.jooq..",
    "com.fasterxml.jackson..",
    "tools.jackson..",
    "org.apache.kafka..",
    "org.slf4j..",
    "org.apache.logging..",
    "ch.qos.logback.."
  };

  private static final String[] AI_PROVIDER_PACKAGES = {
    "com.anthropic..",
    "com.openai..",
    "com.google.genai..",
    "dev.langchain4j..",
    "software.amazon.awssdk.services.bedrockruntime..",
    "com.azure.ai.."
  };

  /** Second package segments that are platform code rather than bounded contexts. */
  private static final Set<String> PLATFORM_SEGMENTS = Set.of("kernel", "platform", "archtest");

  private static final Set<String> INTERNAL_LAYERS =
      Set.of("domain", "application", "adapters", "service");

  private ContractRules() {}

  /** CRC-ARCH-002: service → adapters → application → domain. */
  public static final ArchRule CRC_ARCH_002_DEPENDENCY_DIRECTION =
      layeredArchitecture()
          .consideringOnlyDependenciesInLayers()
          .layer("Domain")
          .definedBy("com.insurtech.*.domain..")
          .layer("Application")
          .definedBy("com.insurtech.*.application..")
          .layer("Adapters")
          .definedBy("com.insurtech.*.adapters..")
          .layer("Service")
          .definedBy("com.insurtech.*.service..")
          .whereLayer("Service")
          .mayNotBeAccessedByAnyLayer()
          .whereLayer("Adapters")
          .mayOnlyBeAccessedByLayers("Service")
          .whereLayer("Application")
          .mayOnlyBeAccessedByLayers("Adapters", "Service")
          .whereLayer("Domain")
          .mayOnlyBeAccessedByLayers("Application", "Adapters", "Service")
          .withOptionalLayers(true)
          .because("CRC-ARCH-002 fixes the dependency direction inside a bounded context");

  /** CRC-ARCH-003: domain and shared kernel are pure Java. */
  public static final ArchRule CRC_ARCH_003_PURE_DOMAIN =
      noClasses()
          .that()
          .resideInAnyPackage(PURE_PACKAGES)
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(FRAMEWORK_PACKAGES)
          .because("CRC-ARCH-003: domain code imports no frameworks, persistence or logging")
          .allowEmptyShould(true);

  /** CRC-ARCH-013: application layer is framework-free; wiring lives in -service. */
  public static final ArchRule CRC_ARCH_013_FRAMEWORK_FREE_APPLICATION =
      noClasses()
          .that()
          .resideInAPackage("com.insurtech..application..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(FRAMEWORK_PACKAGES)
          .because("CRC-ARCH-013: use TransactionPort and other ports instead of framework APIs")
          .allowEmptyShould(true);

  /** CRC-DOM-003: aggregates change through intention-revealing methods, never setters. */
  public static final ArchRule CRC_DOM_003_NO_PUBLIC_SETTERS =
      noMethods()
          .that()
          .arePublic()
          .and()
          .haveNameMatching("set[A-Z].*")
          .should()
          .beDeclaredInClassesThat()
          .resideInAPackage("com.insurtech..domain..")
          .because("CRC-DOM-003: expose business methods such as close(reason, actor)")
          .allowEmptyShould(true);

  /** CRC-DOM-011: insurance agents are Producers; LLM agents are AiAgents. */
  public static final ArchRule CRC_DOM_011_NO_BARE_AGENT =
      noClasses()
          .that()
          .resideInAPackage("com.insurtech..")
          .should()
          .haveSimpleName("Agent")
          .because("CRC-DOM-011: name it Producer (insurance) or AiAgent (LLM)")
          .allowEmptyShould(true);

  /** CRC-MON-001: no floating point in domain models. */
  public static final ArchRule CRC_MON_001_NO_FLOATING_POINT_FIELDS =
      noFields()
          .that()
          .areDeclaredInClassesThat()
          .resideInAnyPackage(PURE_PACKAGES)
          .should()
          .haveRawType(double.class)
          .orShould()
          .haveRawType(float.class)
          .orShould()
          .haveRawType(Double.class)
          .orShould()
          .haveRawType(Float.class)
          .because("CRC-MON-001: use Money, Factor, Percentage or BigDecimal")
          .allowEmptyShould(true);

  /** CRC-MON-001: no floating point in domain method results. */
  public static final ArchRule CRC_MON_001_NO_FLOATING_POINT_RESULTS =
      noMethods()
          .that()
          .areDeclaredInClassesThat()
          .resideInAnyPackage(PURE_PACKAGES)
          .should()
          .haveRawReturnType(double.class)
          .orShould()
          .haveRawReturnType(float.class)
          .orShould()
          .haveRawReturnType(Double.class)
          .orShould()
          .haveRawReturnType(Float.class)
          .because("CRC-MON-001: use Money, Factor, Percentage or BigDecimal")
          .allowEmptyShould(true);

  /** CRC-TIME-001: no legacy date types in domain or application code. */
  public static final ArchRule CRC_TIME_001_NO_LEGACY_DATES =
      noClasses()
          .that()
          .resideInAnyPackage(
              "com.insurtech..domain..", "com.insurtech..application..", "com.insurtech.kernel..")
          .should()
          .dependOnClassesThat()
          .belongToAnyOf(Date.class, Calendar.class, java.sql.Timestamp.class)
          .because("CRC-TIME-001: use Instant, LocalDate or BusinessDateTime")
          .allowEmptyShould(true);

  /** CRC-TIME-002: time comes from an injected Clock. */
  public static final ArchRule CRC_TIME_002_INJECTED_CLOCK =
      noClasses()
          .that()
          .resideInAPackage("com.insurtech..")
          .should()
          .callMethod(Instant.class, "now")
          .orShould()
          .callMethod(LocalDate.class, "now")
          .orShould()
          .callMethod(LocalDateTime.class, "now")
          .orShould()
          .callMethod(ZonedDateTime.class, "now")
          .orShould()
          .callMethod(OffsetDateTime.class, "now")
          .orShould()
          .callMethod(System.class, "currentTimeMillis")
          .because("CRC-TIME-002: pass a java.time.Clock, e.g. Instant.now(clock)")
          .allowEmptyShould(true);

  /** CRC-SEC-001: every command/query handler declares its permission. */
  public static final ArchRule CRC_SEC_001_HANDLERS_ARE_AUTHORIZED =
      methods()
          .that()
          .areDeclaredInClassesThat()
          .resideInAPackage("com.insurtech..application..")
          .and()
          .areDeclaredInClassesThat()
          .haveSimpleNameEndingWith("Handler")
          .and()
          .arePublic()
          .and()
          .haveName("handle")
          .should()
          .beAnnotatedWith("com.insurtech.platform.application.security.Authorized")
          .because("CRC-SEC-001: deny by default; declare @Authorized(action = ...)")
          .allowEmptyShould(true);

  /** CRC-AI-003: model providers are reachable only through the aiharness gateway. */
  public static final ArchRule CRC_AI_003_GATEWAY_ONLY =
      noClasses()
          .that()
          .resideInAPackage("com.insurtech..")
          .and()
          .resideOutsideOfPackage("com.insurtech.aiharness.adapters.gateway..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(AI_PROVIDER_PACKAGES)
          .because("CRC-AI-003: all model calls go through the Model Gateway")
          .allowEmptyShould(true);

  /** CRC-STY-003: no Lombok. */
  public static final ArchRule CRC_STY_003_NO_LOMBOK =
      noClasses()
          .that()
          .resideInAPackage("com.insurtech..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("lombok..")
          .because("CRC-STY-003: use records and explicit code")
          .allowEmptyShould(true);

  /** CRC-OBS-002: no System.out / System.err. */
  public static final ArchRule CRC_OBS_002_NO_STANDARD_STREAMS =
      GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS.allowEmptyShould(true);

  /** CRC-OBS-002: SLF4J, not java.util.logging. */
  public static final ArchRule CRC_OBS_002_NO_JAVA_UTIL_LOGGING =
      GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING.allowEmptyShould(true);

  /** CRC-ERR-002 / CRC-DOM-009: throw typed exceptions. */
  public static final ArchRule CRC_ERR_002_NO_GENERIC_EXCEPTIONS =
      GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS.allowEmptyShould(true);

  /**
   * CRC-ARCH-004 / CRC-ARCH-011: code in {@code com.insurtech.<self>} stays out of other contexts.
   */
  public static ArchRule noReachingInto(String self) {
    DescribedPredicate<JavaClass> otherContextInternals =
        new DescribedPredicate<>("internal classes of other bounded contexts") {
          @Override
          public boolean test(JavaClass javaClass) {
            String[] segments = javaClass.getPackageName().split("\\.");
            return segments.length >= 4
                && segments[0].equals("com")
                && segments[1].equals("insurtech")
                && !segments[2].equals(self)
                && !PLATFORM_SEGMENTS.contains(segments[2])
                && INTERNAL_LAYERS.contains(segments[3]);
          }
        };
    return noClasses()
        .that()
        .resideInAPackage(ROOT + "." + self + "..")
        .should()
        .dependOnClassesThat(otherContextInternals)
        .because("CRC-ARCH-004: reach other contexts only through their -api module or events")
        .allowEmptyShould(true);
  }

  /** Rules every bounded context and BFF must satisfy. */
  public static List<ArchRule> common() {
    return List.of(
        CRC_ARCH_002_DEPENDENCY_DIRECTION,
        CRC_ARCH_003_PURE_DOMAIN,
        CRC_ARCH_013_FRAMEWORK_FREE_APPLICATION,
        CRC_DOM_003_NO_PUBLIC_SETTERS,
        CRC_DOM_011_NO_BARE_AGENT,
        CRC_MON_001_NO_FLOATING_POINT_FIELDS,
        CRC_MON_001_NO_FLOATING_POINT_RESULTS,
        CRC_TIME_001_NO_LEGACY_DATES,
        CRC_TIME_002_INJECTED_CLOCK,
        CRC_SEC_001_HANDLERS_ARE_AUTHORIZED,
        CRC_AI_003_GATEWAY_ONLY,
        CRC_STY_003_NO_LOMBOK,
        CRC_OBS_002_NO_STANDARD_STREAMS,
        CRC_OBS_002_NO_JAVA_UTIL_LOGGING,
        CRC_ERR_002_NO_GENERIC_EXCEPTIONS);
  }

  /** Verifies production classes of {@code com.insurtech.<context>} against the contract. */
  public static void verifyContext(String context) {
    verify(importProduction(ROOT + "." + context), context);
  }

  /** Verifies production classes of {@code com.insurtech.bff.<name>} against the contract. */
  public static void verifyBff(String name) {
    verify(importProduction(ROOT + ".bff." + name), "bff");
  }

  /** Evaluates every rule and reports all violations together. */
  public static void verify(JavaClasses classes, String self) {
    List<ArchRule> rules = new ArrayList<>(common());
    rules.add(noReachingInto(self));
    List<String> failures = new ArrayList<>();
    for (ArchRule rule : rules) {
      EvaluationResult result = rule.evaluate(classes);
      if (result.hasViolation()) {
        failures.add(result.getFailureReport().toString());
      }
    }
    if (!failures.isEmpty()) {
      throw new AssertionError(
          "Code Rule Contract violations:\n\n" + String.join("\n\n", failures));
    }
  }

  private static JavaClasses importProduction(String rootPackage) {
    return new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages(rootPackage);
  }
}
