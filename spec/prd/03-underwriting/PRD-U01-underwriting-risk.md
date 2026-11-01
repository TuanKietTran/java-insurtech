# PRD-U01 — Underwriting, Risk Scoring & Workbench

| Field | Value |
|---|---|
| ID | PRD-U01 |
| Domain | Underwriting & Risk |
| Tier | **PARALLEL** (extensibility target; depends on DEEP P01/P02/F05/F07) |
| Status | Draft v0.1 |
| Owner | Underwriting (TBD) |
| Depends on | P01 (UW rule refs, schemas), P02 (quote/transaction referral hooks), P03 (pricing deviations), F05 (appetite & referral rules), F07 (AI summaries, model governance), S02 (third-party data), F01 (UW authority), F02 |
| Consumed by | P02, X02, A01 |
| Key contract rules | CRC-AI-012, 015; CRC-SEC-003; CRC-AUD-003, 004; CRC-CFG-001, 002 |

## 1. Problem Statement

Underwriters spend much of their time gathering and re-keying data rather than judging risk, while simple risks that rules could clear wait in the same queue as complex ones. Scoring models are often opaque, ungoverned, and impossible to explain to a producer or regulator. The platform needs structured submission intake, data enrichment, explainable risk scoring (rules plus governed ML), referral routing to a workbench, and portfolio monitoring — so straightforward risks flow straight through and underwriters focus on the ones that need judgment.

## 2. Goals

1. **Straight-through rate**: ≥ 60% of personal-lines and ≥ 25% of small-commercial submissions quote without referral (hypothesis by appetite).
2. **Referral turnaround**: p80 referral decision < 4 business hours for small commercial.
3. **Explainable scores**: 100% of risk scores carry reason codes/attributions and model version.
4. **Underwriter productivity**: −40% time per referred submission vs. baseline (data pre-filled, AI summary).
5. **Portfolio visibility**: accumulation by peril/geography refreshed daily, with concentration-limit alerts.

## 3. Non-Goals

- **Actuarial model development** (done in analytics tooling; U01 serves and governs approved models).
- **Reinsurance purchasing/optimization.**
- **Large-account/specialty manuscript underwriting** beyond workbench support (P2).
- **Rating math** (P03).

## 4. Personas & User Stories

- As an **underwriter**, I want a workbench queue of referred submissions with referral reasons, enriched data, prior losses, a risk score with reason codes, and an AI-drafted summary with citations, so that I can decide quickly.
- As an **underwriter**, I want to approve with conditions (subjectivities), counter-offer (different limits/deductibles), apply pricing deviations within my authority, or decline with reason codes.
- As a **producer**, I want to know immediately whether a submission is in appetite and, if referred, its status and what's needed.
- As a **UW manager**, I want to manage appetite rules and authority levels, and see referral volumes and decision consistency.
- As a **portfolio manager / CUO**, I want exposure accumulation by peril zone and concentration alerts.
- As a **model risk officer**, I want model inventory, validation, monitoring, and fairness test results for every scoring model in production.
- *Edge:* data provider down → submission proceeds with "data unavailable" flags; referral rules decide whether to stop.
- *Edge:* duplicate submission from two producers for the same risk → clearance rules apply (first-in or broker-of-record rules).

## 5. Domain Model & Invariants

| Entity | Notes |
|---|---|
| **Submission** | applicant, producer, product, risk objects, requested coverages, documents, source (portal/API/email), clearance status, state |
| **Enrichment Result** | provider, request/response refs, retrieved-at, consent basis, fields mapped to risk objects (with provenance) |
| **Risk Score** | model or rule-set ID + version, inputs hash, score, band, reason codes/attributions, computed-at |
| **Referral** | trigger reasons (rule IDs), assigned UW/queue, required authority, decision (`APPROVED`, `APPROVED_WITH_CONDITIONS`, `COUNTER_OFFERED`, `DECLINED`), conditions/subjectivities, pricing deviations, decided-by + authority |
| **Appetite** | rule sets per product/jurisdiction/class: eligible, refer, decline |
| **Exposure Aggregate** | portfolio projection by peril, zone, LOB, TIV/limits; concentration limits |

**Submission states**: `RECEIVED → CLEARANCE → (CLEARED | BLOCKED_DUPLICATE) → ENRICHING → SCORED → (AUTO_ELIGIBLE | REFERRED | AUTO_DECLINED) → DECIDED`; `→ WITHDRAWN`.

**Invariants**
- INV-U01-1: Auto-decline happens only via deterministic appetite rules (never by ML score or AI alone) and produces reason codes suitable for adverse-action notice.
- INV-U01-2: Any score used in a decision records model/rule version and inputs hash (DecisionRecord).
- INV-U01-3: UW decisions and pricing deviations require authority ≥ the referral's required authority (F01).
- INV-U01-4: Models in production have passed validation and fairness tests and are in the model inventory with an owner (CRC-AI-015).
- INV-U01-5: Consumer report–based data use respects consent/permissible purpose; adverse actions using such data trigger the required notices (X04).

## 6. Requirements

### P0 — Must-have
- **U01-P0-01 Submission intake** from X02, API (ACORD-aligned payloads via S02), and P02 quote flows; documents attached via F06; AI02 extraction of applications/loss runs as proposals.
- **U01-P0-02 Clearance** rules for duplicates/broker-of-record.
- **U01-P0-03 Enrichment** orchestration via S02 adapters (MVR, loss history, property characteristics, geospatial hazard, business data) with consent tracking, caching, and cost budgets per tenant.
- **U01-P0-04 Appetite & referral rules** (F05) per product/jurisdiction/class with reason codes.
- **U01-P0-05 Risk scoring** service: rules-based scores at launch; governed ML models via model registry with explanations; scores as inputs to referral rules.
- **U01-P0-06 Workbench**: referral queue, risk summary, enrichment view, prior policies/claims, score explanation, AI summary (L2), decision actions (approve/conditions/counter/decline), pricing deviations through P03 within authority.
  *Acceptance:* Given a referred submission, the underwriter can decide without leaving the workbench; the decision resumes the quote in P02 automatically.
- **U01-P0-07 Subjectivities** tracked post-bind with due dates and consequences (e.g., cancellation notice if unmet, P02).
- **U01-P0-08 Decision records & adverse-action reasons** for declines and counter-offers.

### P1 — Fast follow
- **U01-P1-01** Portfolio accumulation and concentration monitoring with alerts and binding restrictions (e.g., moratoria ahead of a storm).
- **U01-P1-02** Renewal re-underwriting with change detection (new hazards, loss activity).
- **U01-P1-03** Model monitoring: drift, stability, outcome performance, fairness over time.
- **U01-P1-04** Referral analytics: consistency, decision times, override of scores.

### P2 — Future
- **U01-P2-01** Specialty/manuscript workflows (quote slips, subscription markets).
- **U01-P2-02** Continuous underwriting from IoT/telematics feeds.

## 7. Interfaces & Events
- APIs: `/v1/submissions`, `/v1/submissions/{id}/enrich|score`, `/v1/referrals/{id}/decide`, `/v1/appetite/check`, `/v1/exposures`.
- **Events published**: `underwriting.submission.received.v1`, `…cleared.v1`, `…scored.v1`, `underwriting.referral.created.v1`, `underwriting.referral.decided.v1`, `underwriting.subjectivity.overdue.v1`, `underwriting.exposure.concentration-breached.v1`.
- **Events consumed**: `policy.quote.referred.v1`, `policy.transaction.applied.v1`, `claims.claim.registered.v1` (loss activity).

## 8. AI Integration Points
- AI02 extraction from applications, loss runs, SOVs (L3 proposals).
- Submission summary AI Agent (built on F07; L2) with citations to submission documents and enrichment data.
- AI never declines, approves, or prices (CRC-AI-002); scores are inputs to rules and humans.

## 9. Audit, Security & Compliance
- Model governance evidence (inventory, validation reports, fairness testing); adverse-action reason retention; consumer data permissible-purpose logs.

## 10. Success Metrics

| Metric | Type | Target (hypothesis) |
|---|---|---|
| Straight-through quote rate (personal / small commercial) | Lagging | ≥ 60% / ≥ 25% |
| Referral decision p80 (small commercial) | Leading | < 4 business hours |
| Scores with explanations | Leading | 100% |
| Time per referred submission | Lagging | −40% |
| Quote-to-bind ratio for referred risks | Lagging | Baseline, then +10% |

## 11. Open Questions
- [UW] [Blocking] Launch appetite and referral rules per product.
- [Legal] [Blocking] Permissible third-party data per jurisdiction and required adverse-action notices.
- [Data science] [Non-blocking] Model serving stack and validation standard (internal model-risk policy).

## 12. Dependencies & Phasing
- Requires P02 referral hooks, F05 rules, S02 enrichment adapters. Workbench UI and scoring can proceed in parallel once the submission/referral contract is frozen.
