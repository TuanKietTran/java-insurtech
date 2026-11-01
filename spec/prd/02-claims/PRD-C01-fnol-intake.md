# PRD-C01 — FNOL Multi-Channel Intake

| Field | Value |
|---|---|
| ID | PRD-C01 |
| Domain | Claims |
| Tier | **PARALLEL** |
| Status | Draft v0.1 |
| Owner | Claims Intake (TBD) |
| Depends on | C02 (register claim API), P02 (policy lookup, CoverageSnapshot), P01 (claim settings: required FNOL fields per LOB/coverage), F06 (uploads), F01, F03, AI01 |
| Consumed by | C02, C03, X01, X02, X03, A01 |
| Key contract rules | CRC-IDEM-001, 004; CRC-EVT-*; CRC-SEC-006; CRC-AI-001, 013 |

## 1. Problem Statement

First notice of loss sets the tone and the cost of a claim: incomplete or inaccurate FNOL drives re-contacts, delays, mis-triage, and leakage, and customers judge the insurer most harshly in this moment. Losses get reported through many doors — web, app, phone, producers, partners' APIs, chat — and each door historically had its own forms and data quality. The platform needs one canonical loss report captured consistently across channels, matched to the right policy, de-duplicated, and turned into a registered claim within seconds.

## 2. Goals

1. **Fast acknowledgement**: claimant receives a claim reference within 60 s of submission for ≥ 95% of digital FNOLs.
2. **Complete at first touch**: ≥ 85% of FNOLs contain all LOB-required fields at submission (hypothesis; baseline in pilot).
3. **Digital share**: ≥ 50% of personal-lines FNOL via digital channels within 12 months of launch.
4. **Accurate policy match**: ≥ 98% of FNOLs auto-matched to the correct policy or routed to manual match with no misattachment.
5. **Duplicate prevention**: < 0.5% of registered claims later voided as duplicates.

## 3. Non-Goals

- **Claim handling after registration** (C02/C03).
- **Coverage decisions at intake.** FNOL never tells a claimant they are covered (CRC-AI-002, AI01 guardrails).
- **Telematics crash auto-FNOL** (P2).
- **Call-center telephony platform.** C01 provides the CSR intake UI/API; telephony is external.

## 4. Personas & User Stories

**Policyholder / claimant (digital)**
- As a policyholder, I want to report a car accident from my phone in under 5 minutes with photos so that I can get help quickly.
- As a claimant who isn't our customer (third party), I want to report a claim against our insured without an account.
- As a reporter, I want to save and resume a partially completed report.

**CSR (call center)**
- As a CSR, I want a guided script that adapts to the loss type and LOB so that I capture what adjusters need on the first call.
- As a CSR, I want to find the policy by name, phone, plate, or address when the caller doesn't have a policy number.

**Producer**
- As a producer, I want to report a loss for my client from the producer portal and see its status.

**Partner (API)**
- As a partner (repair network, embedded insurance platform, TPA), I want to submit FNOL via API with idempotency and receive the claim reference synchronously or via webhook.

**AI chat**
- As a policyholder chatting with the virtual assistant, I want it to collect details conversationally and confirm a summary before submitting.

**Edge cases**
- Injury or emergency reported → immediate safety guidance and priority routing to a human.
- Policy not found → loss report still accepted, registered as "unverified policy" for manual match.
- Same loss reported by insured and third party → linked as potential duplicate for adjuster confirmation.

## 5. Domain Model & Invariants

**Loss Report** (owned by `fnol` context; canonical across channels)
- reporter (party data, relationship to insured), channel, policy identifiers or lookup inputs, loss date/time + precision + zone, location (geocoded), loss type / cause of loss (config codes), description, involved parties (drivers, claimants, witnesses), damaged risk objects (vehicles, property, items), injuries (yes/no/unknown + details), authorities involved (police/fire report numbers), attachments (F06 links), consents captured, LOB-specific answers (schema from P01 claim settings), AI extraction metadata (AI01).

**Loss Report states**: `STARTED → SUBMITTED → (MATCHED | UNMATCHED) → CONVERTED` ; `SUBMITTED → DUPLICATE_LINKED` ; `STARTED → ABANDONED` (after retention window).

**Invariants**
- INV-C01-1: A submitted loss report is immutable; later corrections happen on the claim (C02), with the original retained.
- INV-C01-2: Conversion to claim is idempotent: one loss report → at most one claim (or one existing claim it was linked to).
- INV-C01-3: Every channel produces the same canonical schema version; channel-specific data goes in a namespaced extension.

## 6. Requirements

### P0 — Must-have

**C01-P0-01 Canonical loss report schema** versioned in `fnol-api`, with LOB-specific question sets driven by P01 claim settings (no code per LOB).

**C01-P0-02 Channels:** customer web (X01), mobile (X03), producer portal (X02), CSR desktop, partner API (S02 auth), AI chat (AI01). All use the same intake API.

**C01-P0-03 Policy match.** Exact match by policy number; assisted search by name/DOB/address/phone/VIN/plate (S01, permission-aware); confidence + confirmation step; unmatched path.
*Acceptance:* Given an unmatched report, the claim is registered with `POLICY_UNVERIFIED` and a manual-match work item is created; no coverage snapshot is bound until matched.

**C01-P0-04 Duplicate detection.** Candidate duplicates by policy + loss date ± window + location/vehicle similarity; linked for adjuster decision; never silently merged.

**C01-P0-05 Registration.** Submitted + matched (or unverified) reports call C02 `RegisterClaim` with a derived idempotency key; claim number returned to reporter.
*Acceptance:* p95 submission-to-claim-number < 60 s (digital); retries never create two claims.

**C01-P0-06 Acknowledgement.** Immediate confirmation (screen + X04 message) with claim number, next steps, and contact info; regulatory acknowledgement timer starts (C02).

**C01-P0-07 Save & resume** drafts for authenticated reporters and via secure link for unauthenticated ones; draft retention per R01.

**C01-P0-08 Emergency/injury handling.** Injury or safety indicators trigger guidance text, priority flag, and immediate human routing (C03).

**C01-P0-09 Attachments.** Photos/documents upload during FNOL via F06 with provenance (device, capture time, geo).

**C01-P0-10 Accessibility & language.** WCAG 2.2 AA on digital channels; launch languages configurable; reporter language stored as party preference.

### P1 — Fast follow
- **C01-P1-01** Email/document FNOL ingestion (AI01/AI02 extract → proposal → CSR confirm).
- **C01-P1-02** Guided photo capture (angles, damage close-ups) with quality checks (X03).
- **C01-P1-03** Partner webhooks for status updates.
- **C01-P1-04** Pre-FNOL triage questions to route glass/roadside/towing directly to vendors (V01).
- **C01-P1-05** CAT-mode intake (simplified flow, CAT event tagging, surge capacity).

### P2 — Future
- **C01-P2-01** Telematics crash-detected FNOL with consented data.
- **C01-P2-02** Voice-channel AI intake with real-time transcript extraction.

## 7. Interfaces & Events
- APIs: `POST /v1/loss-reports` (draft), `PUT /v1/loss-reports/{id}`, `POST /v1/loss-reports/{id}/submit`, `POST /v1/loss-reports/{id}/match`, `GET /v1/loss-reports/{id}`.
- **Events published**: `fnol.loss-report.submitted.v1`, `fnol.loss-report.matched.v1`, `fnol.loss-report.unmatched.v1`, `fnol.loss-report.duplicate-suspected.v1`, `fnol.loss-report.converted.v1`.
- **Events consumed**: `claims.claim.registered.v1` (confirm conversion).

## 8. AI Integration Points
- **AI01** extracts structured fields from chat, free text, transcripts, and emails; proposes values with confidence and source spans; reporter/CSR confirms (L2/L3). AI01 never states coverage.
- **AI02** classifies uploaded photos/docs at intake (L2).

## 9. Audit, Security & Compliance
- Loss report submission and conversion audited; consents (e.g., SMS updates, recorded chat) captured per R01.
- Unauthenticated third-party reporting protected by bot defense and rate limits; no policy details disclosed to unauthenticated reporters.
- Acknowledgement-deadline timers per jurisdiction (C02).

## 10. Success Metrics

| Metric | Type | Target (hypothesis) |
|---|---|---|
| Submission → claim number p95 (digital) | Leading | < 60 s |
| FNOL completeness at submission | Leading | ≥ 85% |
| Digital FNOL share (personal lines) | Lagging | ≥ 50% at 12 months |
| Auto policy-match precision | Leading | ≥ 99.5% (no misattachment) |
| Claims voided as duplicates | Lagging | < 0.5% |
| FNOL abandonment rate | Leading | < 20% of started digital reports |

## 11. Open Questions
- [Product] [Blocking] Launch channels and LOBs for FNOL; third-party claimant self-reporting at launch?
- [Legal] [Non-blocking] Consent wording for chat recording and SMS updates per jurisdiction.
- [Claims] [Non-blocking] Duplicate-detection window and similarity thresholds per LOB.

## 12. Dependencies & Phasing
- Requires C02 `RegisterClaim` contract and P02 policy lookup; can start against stubs immediately.
- Channel UIs (X01, X02, X03, CSR) can be built in parallel once the intake API is frozen.
