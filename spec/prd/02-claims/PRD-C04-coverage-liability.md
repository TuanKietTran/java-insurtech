# PRD-C04 — Coverage Determination & Liability Assessment

| Field | Value |
|---|---|
| ID | PRD-C04 |
| Domain | Claims |
| Tier | **DEEP** |
| Status | Draft v0.1 |
| Owner | Claims Core (TBD) |
| Depends on | P02 (CoverageSnapshot), P01 (coverage patterns, claim settings, forms), F05 (coverage & liability rule sets), F02 (DecisionRecords), F06 (forms, evidence), C02, C05 (limit consumption), AI03 |
| Consumed by | C02 (closure guards), C05 (payable amounts, deductibles, limits), X04 (denial/ROR letters), R02, A01 |
| Key contract rules | CRC-CFG-003; CRC-TIME-006, 007; CRC-AUD-003, 004; CRC-AI-002, 008, 012; CRC-TST-007 |

## 1. Problem Statement

Whether a loss is covered — and for how much — is the most consequential decision in a claim and the one most often challenged by customers, attorneys, and regulators. When coverage is decided from an adjuster's reading of a policy PDF, outcomes vary by adjuster, limits and deductibles are misapplied, aggregate limits are over-paid, and denials cite the wrong wording. The platform needs deterministic, explainable coverage determination driven by the exact policy version and forms in force at the loss, with AI assisting interpretation but never deciding, and with liability assessment for third-party claims expressed as configurable, jurisdiction-aware rules.

## 2. Goals

1. **Deterministic**: same coverage snapshot + same facts + same rule version ⇒ identical coverage outcome and explanation (100% golden replay).
2. **Explainable**: 100% of coverage positions cite the coverage, form, and clause references they rely on; 100% of denials/RORs carry approved wording citations.
3. **Correct limits and deductibles**: 0 payments exceeding available per-occurrence/aggregate/sublimits; deductible misapplication rate < 0.1% in QA sampling.
4. **Consistency**: variance between adjusters on identical fact patterns reduced by 50% vs. baseline (QA calibration study).
5. **Speed**: ≥ 60% of personal-lines claim lines receive an automated *recommended* coverage position at registration.

## 3. Non-Goals

- **Automatic denials.** Adverse positions always require a human with coverage authority (CRC-AI-002 for AI; rule-recommended denials still require human confirmation).
- **Coverage litigation strategy** and declaratory judgment management.
- **Damage valuation/estimating** (vendor/estimating platforms via V01/S02); C04 consumes valuations.
- **Bodily-injury general damages evaluation engines** (P2).

## 4. Personas & User Stories

**Adjuster**
- As an adjuster, I want the system to show, per claim line, whether the policy was in force at the loss time, whether the coverage, risk object, and peril are covered, which exclusions/conditions may apply, and the applicable limits and deductible, each with citations, so that I can confirm or investigate.
- As an adjuster, I want to ask questions about the policy wording ("Does this policy exclude wear and tear for the roof?") and get answers grounded in the actual form edition (AI03).
- As an adjuster, I want to issue a reservation of rights while I investigate so that we preserve our position without delaying handling.

**Coverage counsel / coverage specialist**
- As a coverage specialist, I want a coverage determination record I can defend: facts considered, rules fired, wording cited, human rationale, approvals.

**Liability adjuster (third-party)**
- As a liability adjuster, I want to record fault percentages per party with the applicable negligence doctrine for the jurisdiction computed automatically so that settlement values reflect comparative fault correctly.

**Claimant (indirect)**
- As a claimant receiving a denial, I want the letter to quote the specific policy language and explain my rights so that I understand the decision.

**Edge cases**
- Loss time ambiguous across policy inception/cancellation (C02 flags two candidate snapshots).
- Coverage code on the claim line not present on the snapshot.
- Aggregate limit shared across multiple claims under the same term — concurrent payments.
- Late notice condition with jurisdiction-specific prejudice requirement.
- Retroactive endorsement after determination → re-evaluation.

## 5. Domain Model & Invariants

| Entity | Notes |
|---|---|
| **Coverage Evaluation** | claim line, coverage snapshot ID + hash, facts (loss cause, location, risk object, dates, notice date, investigation findings), rule-set version, outcome components (below), explanation, evaluated-at |
| **Coverage Position** (per claim line, versioned) | `PENDING`, `COVERED`, `PARTIALLY_COVERED`, `NOT_COVERED`, `RESERVATION_OF_RIGHTS`; basis (evaluation ref + human decision), cited clauses, applicable limits (ref to limit definitions), deductible, sublimits, coinsurance, effective-from, decided-by + authority, supersedes |
| **Limit Consumption** (projection) | policy term × limit definition (per occurrence / aggregate / per person) → consumed (paid; optionally reserved), available; fed by C05/F04 events |
| **Liability Assessment** (per third-party claim line, versioned) | parties & fault %, doctrine (config by jurisdiction: pure comparative, modified 50%, modified 51%, contributory, etc.), recoverability factor, evidence refs, decided-by |
| **ROR / Denial Record** | position ref, letter (F06 document) from approved template, cited wording, recipients, sent-at, proof (X04) |

**Coverage evaluation components** (each with pass/fail/unknown + citations)
1. Policy in force at loss instant (handles precision and lapse windows)
2. Insured / claimant eligibility (named insured, permissive user, resident relative — per form definitions)
3. Risk object covered (scheduled vehicle/location/item, newly acquired rules)
4. Peril / cause of loss covered
5. Exclusions potentially applicable (requiring facts)
6. Conditions (notice, cooperation, proof of loss) with jurisdictional modifiers
7. Limits applicable and available (incl. aggregate consumption)
8. Deductible / retention / coinsurance application
9. Other insurance / priority (P1)

**Coverage Position state machine**: `PENDING → (COVERED | PARTIALLY_COVERED | NOT_COVERED | RESERVATION_OF_RIGHTS)`; `RESERVATION_OF_RIGHTS → (COVERED | PARTIALLY_COVERED | NOT_COVERED)`; any resolved position → *revised* position (new version) with reason and authority.

**Invariants**
- INV-C04-1: Every position references the coverage snapshot (ID + hash) and the rule-set version used; positions are versioned, never overwritten.
- INV-C04-2: `NOT_COVERED` and `PARTIALLY_COVERED` require a human with coverage authority, cited clauses from the forms on the snapshot, and an approved letter template before communication.
- INV-C04-3: `RESERVATION_OF_RIGHTS` requires cited clauses and issuance of an ROR letter within the configured deadline.
- INV-C04-4: Available limit is computed from limit consumption at payment approval time with serialization per (term, limit definition) — no two concurrent approvals can jointly exceed the limit.
- INV-C04-5: When rules output a determinate result and a human decides differently, the decision is an override with reason (CRC-AUD-004). AI suggestions never set a position (CRC-AI-002).
- INV-C04-6: Liability fault percentages across parties for a claim line sum to 100%; doctrine applied is the one configured for the loss jurisdiction and loss date.
- INV-C04-7: A coverage snapshot rebind (C02) marks dependent positions `REVIEW_REQUIRED` (flag) until re-evaluated.

## 6. Requirements

### P0 — Must-have

**C04-P0-01 Coverage evaluation service.** Evaluate components 1–8 via coverage rule sets (F05) configured per product version and coverage pattern (P01), using the snapshot and facts; returns component results, recommended position, missing facts list, and citations.
*Acceptance:*
- Given a golden scenario suite per launch product (≥ 50 scenarios each, incl. edge cases), evaluation outputs match expected results byte-for-byte across runs.
- Given missing facts needed for an exclusion, the result is `PENDING` with a `missingFacts` list that generates investigation work items (C03).

**C04-P0-02 Coverage positions** with versioning, authority checks, and decision records; UI shows recommendation vs. decision and requires override reason when different.

**C04-P0-03 ROR & denial workflow.** Templates per jurisdiction (F06/X04) with mandatory clause citations, approval by coverage authority, deadline tracking (C02), proof of sending; AI06 may draft explanatory sections only within template-permitted regions (L3).

**C04-P0-04 Limits & deductibles engine.** Apply limit structures (per occurrence/person/claim/aggregate/sublimit) and deductible types from the snapshot; compute payable amounts for C05 settlements and payments; maintain limit consumption with concurrency guarantees (INV-C04-4).
*Acceptance:* Given two concurrent payment approvals that together exceed remaining aggregate, exactly one succeeds and the other is rejected with `LIMIT_EXHAUSTED` (property test with concurrency).

**C04-P0-05 Liability assessment.** Record fault allocation per party with evidence; compute recoverable share by jurisdiction doctrine (F05 rules, effective-dated); feed settlement evaluation (C05) and subrogation potential (C05 recovery).

**C04-P0-06 Re-evaluation triggers.** Snapshot rebind, new facts (investigation results), retroactive policy change, or fact corrections trigger re-evaluation and a review task if the recommended position changes.

**C04-P0-07 Explanation view.** For each position: components, fired rules (annotations), cited form clauses (linked to S01 wording spans), facts used, human rationale, approvals — embedded in F02 "Why" view.

### P1 — Fast follow
- **C04-P1-01** Other-insurance and priority-of-coverage rules (primary/excess, multiple policies).
- **C04-P1-02** Coverage calibration tool: present identical fact patterns to adjusters for QA consistency scoring.
- **C04-P1-03** Coverage position templates for common scenarios (one-click confirm with citations).
- **C04-P1-04** Reserved limit consumption (include outstanding reserves in availability for warning thresholds).

### P2 — Future
- **C04-P2-01** Bodily injury evaluation support (injury codes, treatment, jurisdiction verdict data via S02) as advisory ranges.
- **C04-P2-02** Reinsurance treaty attachment checks (per occurrence retention vs. CAT event aggregation).

## 7. Interfaces & Events
- APIs: `POST /v1/claim-lines/{id}/coverage-evaluations`, `POST /v1/claim-lines/{id}/coverage-positions`, `GET /v1/claim-lines/{id}/coverage-positions`, `POST /v1/claim-lines/{id}/liability-assessments`, `GET /v1/limit-availability?termId=&limitDefinition=`, internal `LimitPort.reserveConsumption(...)` used by C05 at approval.
- **Events published**: `claims.coverage.evaluated.v1`, `claims.coverage.determined.v1`, `claims.coverage.revised.v1`, `claims.coverage.ror-issued.v1`, `claims.coverage.denial-issued.v1`, `claims.liability.assessed.v1`, `claims.limit.exhausted.v1`, `claims.limit.threshold-reached.v1`.
- **Events consumed**: `claims.claim.coverage-snapshot-rebound.v1`, `claims.claim.loss-details-corrected.v1`, `policy.version.retroactively-changed.v1`, `ledger.journal-entry.posted.v1` (paid amounts for consumption), `documents.extraction.verified.v1` (facts).

## 8. AI Integration Points
- **AI03** Coverage & Policy Q&A (RAG) for adjusters, grounded in the snapshot's form editions; outputs advisory answers with citations (L2).
- **AI02** extracts facts from evidence (police report narrative, estimates) as proposals for verification; verified facts feed evaluation.
- AI suggestions can appear as input facts flagged `AI_SUGGESTED` but cannot satisfy a fact required for an adverse determination until verified by a human.

## 9. Audit, Security & Compliance
- DecisionRecords for every evaluation and position; denial/ROR letters retained as issued documents with proof of mailing.
- Unfair-claims-practices compliance: timely decisions (C02 deadlines), cited wording, consistent application.
- Explanations exportable for regulators and litigation (F02 evidence pack).

## 10. Success Metrics

| Metric | Type | Target (hypothesis) |
|---|---|---|
| Golden replay determinism | Leading | 100% |
| Positions with citations | Leading | 100% |
| Payments exceeding available limits | Lagging | 0 |
| Deductible misapplication (QA) | Lagging | < 0.1% |
| Adjuster variance on calibration set | Lagging | −50% vs. baseline |
| Lines with automated recommended position at registration | Leading | ≥ 60% (personal lines) |
| Overturned denials (complaint/appeal) | Lagging | Baseline, then −30% |

## 11. Open Questions
- [Claims/Legal] [Blocking] Coverage rule content for launch products and the clause-reference scheme (form + section + paragraph IDs).
- [Legal] [Blocking] Jurisdictional denial/ROR letter requirements and deadlines.
- [Engineering] [Blocking] Limit-consumption concurrency approach (row lock per limit bucket vs. ledger-backed reservation) — ADR.
- [Actuarial] [Non-blocking] Should aggregate availability include outstanding reserves for warnings (P1) or only paid?

## 12. Dependencies & Phasing
- **First**: snapshot consumption, coverage evaluation for components 1, 3, 4, 7, 8 (in force, risk object, peril, limits, deductibles), positions with authority; limit consumption with concurrency.
- **Then**: exclusions/conditions with missing-facts loop, ROR/denial workflow, liability assessment.
- **Parallel**: AI03 in shadow, calibration tooling, other-insurance rules.
