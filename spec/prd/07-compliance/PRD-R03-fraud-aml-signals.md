# PRD-R03 — Fraud, Sanctions & AML Signals

| Field | Value |
|---|---|
| ID | PRD-R03 |
| Domain | Compliance — Anti-fraud & AML |
| Tier | **PARALLEL** (sanctions screening before payment release is a go-live gate for F04) |
| Status | Draft v0.1 |
| Owner | SIU & Financial Crime (TBD) |
| Depends on | C02/C03 (claims, SIU referral), C05/F04 (payment holds, screening gate), P02/B01 (policy & billing patterns), F06 (perceptual hashes, metadata), F05 (red-flag rules), S02 (sanctions lists, industry fraud databases), F07/AI05 |
| Consumed by | C03 (triage, SIU), C05 (holds), F04 (screening), U01 (application fraud), A01 |
| Key contract rules | CRC-AI-002, 012, 015; CRC-AUD-003; CRC-SEC-001 |

## 1. Problem Statement

Insurance fraud — staged accidents, inflated repairs, provider schemes, application misrepresentation, premium-refund laundering — raises costs for every honest customer, while sanctions and AML obligations make paying the wrong party a legal violation. Detection that relies on adjuster intuition misses organized patterns across claims; detection that auto-denies on a model score is unfair and legally risky. The platform needs explainable fraud signals (rules, anomaly scores, network links, document reuse), an SIU case workflow, mandatory sanctions screening of parties and payees before money moves, and AML pattern detection — with signals informing humans, never deciding outcomes alone.

## 2. Goals
1. **Sanctions compliance**: 100% of payees screened before release; 0 payments released to confirmed sanctioned parties.
2. **Detection quality**: SIU referral acceptance rate ≥ 40% (hypothesis) with confirmed-fraud hit rate baseline then +25%.
3. **Low friction**: < 3% of legitimate claims delayed by fraud holds (false positives) beyond 2 business days.
4. **Explainability**: 100% of fraud scores and flags carry reason codes and evidence links.
5. **Mandated reporting**: 100% of reportable fraud referrals filed with fraud bureaus within required timelines.

## 3. Non-Goals
- **Automatic claim denial or policy rescission based on fraud signals** (human decision only, CRC-AI-002 and INV-R03-1).
- **Full financial-crime transaction monitoring** suite beyond insurance-specific patterns.
- **Law-enforcement case management.**

## 4. Personas & User Stories
- As an **SIU investigator**, I want referred claims with all signals, network graph, document reuse matches, and an AI summary with evidence links, so that I can decide to open a case.
- As an **adjuster**, I want to see that a claim has a fraud indicator (without accusatory language) and what verification step is recommended.
- As a **compliance analyst**, I want sanctions hits on payees placed on hold with match details so that I can clear false positives or confirm and block.
- As a **financial crime officer**, I want alerts for patterns such as overpayment followed by refund to a different instrument, or early cancellation of large policies paid in cash equivalents.
- As an **SIU manager**, I want to measure rule and model performance (hit rates, false positives) to tune them.
- *Edge:* same photo appears on two unrelated claims (F06 perceptual hash) → signal on both.
- *Edge:* claimant shares phone/bank account with parties on prior SIU-confirmed claims → network signal.

## 5. Domain Model & Invariants
| Entity | Notes |
|---|---|
| **Signal** | subject (claim, claim line, party, policy, payment, vendor), type (`RULE_RED_FLAG`, `ANOMALY_SCORE`, `NETWORK_LINK`, `DOCUMENT_REUSE`, `EXTERNAL_DB_MATCH`, `AML_PATTERN`), score/severity, reason codes, evidence refs, source version |
| **Entity Resolution Graph** | parties, phones, emails, addresses, bank accounts (tokenized), vehicles, devices, providers → links with confidence |
| **Screening Check** | party/payee, lists checked + versions, result (`CLEAR`, `POTENTIAL_MATCH`, `CONFIRMED_MATCH`), adjudication |
| **Hold** | subject (payment order, claim line), reason, placed-by, release conditions |
| **SIU Case** | referral(s), investigator, plan, findings, outcome (`NO_FRAUD`, `SUSPECTED`, `CONFIRMED`, `REFERRED_TO_AUTHORITIES`), bureau filings |
| **AML Alert** | pattern, transactions, disposition, filing support |

**Invariants**
- INV-R03-1: Signals never change coverage positions, reserves, or payments directly; they may place a **hold** on payment release that a human with authority must clear.
- INV-R03-2: Payment orders cannot be released without a `CLEAR` (or adjudicated-cleared) screening result on the current payee data (F04 INV-F04-6).
- INV-R03-3: Every score is versioned with reason codes; protected attributes and flagged proxies are excluded (CRC-AI-015).
- INV-R03-4: SIU-related data is visible only to SIU-permitted roles and never to claimants/vendors.

## 6. Requirements
### P0
- **R03-P0-01 Sanctions screening** of insureds, claimants, payees, vendors at onboarding/registration and before each payment release; list updates via S02; rescreen on list change; adjudication workflow.
  *Acceptance:* Given a payee potential match, the payment order goes `ON_HOLD`; release is impossible until adjudicated `CLEAR` by a compliance principal (SoD with requester).
- **R03-P0-02 Red-flag rules** (F05) at FNOL, during handling, and pre-payment (e.g., loss shortly after inception or coverage increase, prior claims frequency, late reporting, inconsistent dates/locations).
- **R03-P0-03 Fraud score** service combining rules and an anomaly model (governed, versioned, explainable).
- **R03-P0-04 Network signals** via entity resolution over parties/contacts/bank tokens/vehicles across claims and policies.
- **R03-P0-05 Document reuse signals** from F06 perceptual hashes and metadata inconsistencies (EXIF vs. reported loss time/location).
- **R03-P0-06 Holds** on payments with reason and release workflow.
- **R03-P0-07 SIU referral & case management** (intake from C03, triage, assignment, findings, outcomes, bureau filing records).
- **R03-P0-08 AML patterns** for premium overpayment/refund, rapid cancellation with refund, third-party payments, structuring indicators; alert disposition with filing support where applicable.
### P1
- **R03-P1-01** Industry fraud database integrations (S02) for claim history matching.
- **R03-P1-02** Graph investigation UI for SIU.
- **R03-P1-03** Rule/model performance dashboards and tuning workflow with back-testing (F05-P1-01).
- **R03-P1-04** Application fraud signals for U01 (misrepresentation indicators).
### P2
- **R03-P2-01** Consortium data sharing under legal agreements.
- **R03-P2-02** Real-time device intelligence at portal/mobile login and FNOL.

## 7. Interfaces & Events
- APIs: `/v1/screening-checks`, `/v1/signals?subject=`, `/v1/holds`, `/v1/siu-cases`, `/v1/aml-alerts`.
- **Published**: `fraud.screening.completed.v1`, `fraud.signal.raised.v1`, `fraud.score.computed.v1`, `fraud.hold.placed.v1`, `…released.v1`, `fraud.siu-referral.accepted.v1`, `fraud.siu-case.closed.v1`, `fraud.aml-alert.raised.v1`.
- **Consumed**: `claims.claim.registered.v1`, `claims.party.added.v1`, `ledger.payment-order.approved.v1`, `documents.document.available.v1`, `billing.payment.received.v1`, `billing.refund.approved.v1`, `policy.term.cancelled.v1`.

## 8. AI Integration Points
- **AI05** fraud signal summarization for SIU and adjusters (internal only, non-accusatory, cited, L2).
- AI02 metadata/document consistency checks as signals (L2).

## 9. Audit, Security & Compliance
- Screening results, adjudications, holds, SIU decisions audited with reasons; SIU data access restricted and read-audited; bureau filings retained.

## 10. Success Metrics
| Metric | Type | Target (hypothesis) |
|---|---|---|
| Payees screened before release | Leading | 100% |
| Payments to confirmed sanctioned parties | Lagging | 0 |
| SIU referral acceptance | Lagging | ≥ 40% |
| Legitimate claims held > 2 business days | Leading | < 3% |
| Signals with reason codes | Leading | 100% |

## 11. Open Questions
- [Compliance] [Blocking] Sanctions lists and screening provider per market; fuzzy-match thresholds.
- [Legal] [Blocking] Fraud bureau reporting obligations per jurisdiction; AML obligations by LOB (heavier for life/annuity, lighter for P&C).
- [SIU] [Non-blocking] Launch red-flag rule set per LOB.

## 12. Dependencies & Phasing
- **Gate**: R03-P0-01 screening + R03-P0-06 holds must be live before F04 releases real payments.
- Scores, network, document reuse, SIU case management in parallel.
