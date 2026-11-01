# PRD-F05 — Rules & Decision Engine

| Field | Value |
|---|---|
| ID | PRD-F05 |
| Domain | Foundation — Configuration |
| Tier | **DEEP** |
| Status | Draft v0.1 |
| Owner | Platform Configuration (TBD) |
| Depends on | F01, F02 (DecisionRecords), F03 |
| Consumed by | P01 (eligibility, validation), P02 (cancellation notice rules), P03 (rating rules), U01 (referral, appetite), C03 (triage, assignment, SLA), C04 (coverage, liability), C05 (initial reserves, authority routing), B01 (delinquency, allocation), R03 (fraud red flags), X04 (routing, required notices), F04 (posting rules) |
| Key contract rules | CRC-CFG-001…005; CRC-TIME-007; CRC-AUD-003; CRC-AI-012; CRC-TST-007 |

## 1. Problem Statement

Insurance behavior varies by product, jurisdiction, effective date, and tenant: which coverages apply, when a cancellation notice must be mailed, which claims fast-track, what triggers an underwriting referral. Hard-coding this variability makes every new product a code release and makes decisions impossible to explain. The platform needs one deterministic, versioned, effective-dated decision service that business analysts can author safely and that explains every outcome.

## 2. Goals

1. **Deterministic and explainable**: identical (rule-set version, inputs) always produces identical output and explanation; every evaluation used for a business decision yields a DecisionRecord.
2. **Business-authored**: ≥ 80% of rule changes are authored and published by trained business analysts without engineering involvement.
3. **Safe change**: 100% of published rule versions have passing bundled test cases; regressions are caught pre-publish via golden files and back-testing.
4. **Fast**: p99 evaluation < 20 ms for typical decision tables (≤ 500 rows) in-process or via sidecar.

## 3. Non-Goals

- **ML scoring.** Models are served by U01/R03/F07 infrastructure; rules may consume scores as input facts.
- **Workflow orchestration.** Rules decide; process managers (F03) act.
- **General-purpose programming for business users.** Rule expressiveness is intentionally bounded (no loops, no I/O).
- **Rating algorithms.** P03 owns rating plans (it may call F05 for eligibility/surcharge rules).

## 4. Personas & User Stories

**Product / business analyst**
- As a product analyst, I want to edit a decision table (e.g., fast-track eligibility) in a governed editor, attach test cases, and submit for approval so that I can change behavior without a release.
- As an analyst, I want to see the impact of a rule change against last quarter's claims (back-test) before publishing so that I don't break operations.
- As an analyst, I want to schedule a rule version to take effect for new business on date X and for renewals on date Y.

**Approver (product owner / compliance)**
- As a compliance reviewer, I want to see a diff between rule versions and the test results before approving.

**Adjuster / underwriter (consumer of outcomes)**
- As an adjuster, I want to see which rules fired and why when a claim was routed to SIU or a coverage check failed.

**Developer**
- As a developer, I want a typed client with a declared fact schema per rule set so that missing inputs fail fast at evaluation time.

**Edge cases**
- Inputs missing or of the wrong type → typed error, not a default outcome.
- No rule version effective on the requested date → explicit `NO_EFFECTIVE_VERSION` error.
- Two rows match in a unique-hit table → authoring-time validation error, never a runtime coin flip.

## 5. Domain Model & Invariants

| Entity | Key attributes |
|---|---|
| **Rule Set** | id, tenant, purpose (`ELIGIBILITY`, `COVERAGE`, `LIABILITY`, `TRIAGE`, `ASSIGNMENT`, `REFERRAL`, `FRAUD_FLAG`, `NOTICE`, `ALLOCATION`, `POSTING`, …), fact schema (JSON Schema), output schema, owner |
| **Rule Set Version** | version number, content (decision tables / DMN models / expression rules), effective period (per channel: new business, renewal, claims loss date), content hash, test cases, lifecycle state |
| **Decision Table** | inputs, outputs, hit policy (`UNIQUE`, `FIRST`, `PRIORITY`, `COLLECT`), rows with annotations and reason codes |
| **Evaluation** | ruleSetRef, asOf, facts → DecisionResult |
| **DecisionResult** | outcome(s), firedRules (row IDs + annotations + reason codes), version + hash, evaluation time, warnings |

**Rule Set Version lifecycle**: `DRAFT → IN_REVIEW → APPROVED → PUBLISHED → RETIRED` (+ `REJECTED` back to `DRAFT`). Published versions are immutable (CRC-CFG-004).

**Invariants**
- INV-F05-1: Evaluation is pure: no I/O, no clock, no randomness; effective date is an explicit input (CRC-TIME-007).
- INV-F05-2: Published content and its hash never change.
- INV-F05-3: A version can publish only with all bundled test cases passing and four-eyes approval (author ≠ approver).
- INV-F05-4: For any (rule set, channel, date) at most one version is effective.
- INV-F05-5: Every evaluation used by a business decision is persisted as a DecisionRecord with version + hash + facts hash.

## 6. Requirements

### P0 — Must-have

**F05-P0-01 Evaluation API.** `evaluate(ruleSetRef, asOf, channel, facts) → DecisionResult` as an embeddable library and service; validates facts against schema; returns explanation.
*Acceptance:* Given the same version and facts, 10,000 repeated evaluations return byte-identical results (golden test).

**F05-P0-02 Decision tables + expression language.** Support DMN-compatible decision tables and a bounded expression language (FEEL-like) with date arithmetic on supplied dates, numeric/money comparisons, list membership.

**F05-P0-03 Authoring with validation.** Editor (UI + Git-backed config-as-code) with static checks: overlapping rows for unique-hit, unreachable rows, missing coverage of input domains (gap analysis), schema mismatches.

**F05-P0-04 Versioning & effective dating.** Per-channel effective dates (e.g., new business vs. renewal vs. loss date); scheduled publication; retirement.

**F05-P0-05 Test cases & golden files.** Each version carries test cases (facts → expected outcome); CI and the publish workflow run them; diffs vs. previous version shown.

**F05-P0-06 Governance workflow.** Draft → review → approve → publish with four-eyes, comments, and audit; emergency rollback = publish previous content as new version.

**F05-P0-07 Explanation.** Fired rules with business-readable annotations and reason codes, input values used, and (for coverage) references to policy form/clause IDs supplied in annotations.

**F05-P0-08 Decision recording.** Library emits DecisionRecords via F02 when the caller marks the evaluation as a business decision.

### P1 — Fast follow
- **F05-P1-01** Back-testing/simulation: run a draft version over a historical fact set (from DecisionRecords) and report outcome distribution shifts.
- **F05-P1-02** Shadow evaluation: run a candidate version alongside the published one in production, logging differences only.
- **F05-P1-03** Rule usage analytics: hit frequency, never-fired rows, outcome trends.
- **F05-P1-04** Reusable rule fragments/libraries across products with dependency versioning.

### P2 — Future
- **F05-P2-01** Natural-language rule drafting assist (AI, L2): drafts table rows from guideline text for analyst editing; never publishes.
- **F05-P2-02** Formal verification of selected rule properties (e.g., "no row denies coverage without a citation").

## 7. Interfaces & Events
- `RulesPort.evaluate(...)`; admin APIs `/v1/rule-sets`, `/v1/rule-sets/{id}/versions`, `…/submit|approve|publish|retire`, `/v1/rule-sets/{id}/versions/{v}/test-runs`.
- **Events published**: `rules.rule-set-version.submitted.v1`, `…approved.v1`, `…published.v1`, `…retired.v1`.

## 8. AI Integration Points
- AI recommendations may be supplied as *input facts* (e.g., AI triage complexity), but rules decide hard constraints (CRC-AI-012).
- P2 drafting assist (above).

## 9. Audit, Security & Compliance
- Rule changes fully audited; regulators can be shown the exact rule version and explanation for any decision.
- Rule sets affecting rates/forms may require regulatory filing references (P01) before publish.
- Fairness: rule fact schemas flag protected attributes; using them in UW/triage/fraud rule sets is blocked (CRC-AI-015 applies to rules as well).

## 10. Success Metrics

| Metric | Type | Target (hypothesis) |
|---|---|---|
| Rule changes without engineering | Lagging | ≥ 80% |
| Published versions with passing tests | Leading | 100% |
| Production incidents caused by rule changes | Lagging | ≤ 1 per quarter, none financial |
| Evaluation p99 | Leading | < 20 ms |
| Decisions with explanation available | Leading | 100% |

## 11. Open Questions
- [Engineering] [Blocking] Engine: embed an existing DMN engine (e.g., Drools/Kogito DMN) vs. purpose-built decision tables — ADR on determinism, explanation quality, performance.
- [Product] [Non-blocking] Which rule sets require compliance approval in addition to product approval?

## 12. Dependencies & Phasing
- **First**: evaluation library, decision tables, versioning, test cases, DecisionRecord emission — needed by P01, C04, C03.
- **Then**: authoring UI, governance workflow (Git-backed path can come first).
- **Parallel**: back-testing, shadow mode, analytics.
