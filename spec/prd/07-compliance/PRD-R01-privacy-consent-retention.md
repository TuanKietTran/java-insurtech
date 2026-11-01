# PRD-R01 — Privacy, Consent & Data Retention

| Field | Value |
|---|---|
| ID | PRD-R01 |
| Domain | Compliance, Audit & Regulatory |
| Tier | **PARALLEL** — but **day-1 hooks are mandatory**: data classification, field encryption with per-subject keys, and retention classes must exist before any DEEP context stores personal data |
| Status | Draft v0.1 |
| Owner | Privacy Engineering + DPO (TBD) |
| Depends on | F01, F02, F03, F06; KMS/HSM infrastructure |
| Consumed by | Every context storing personal data; F07 (provider data policy); X04 (consent); X01 (DSR) |
| Key contract rules | CRC-PER-006, 009; CRC-SEC-006, 012; CRC-EVT-007; CRC-AI-011; CRC-TST-010 |

## 1. Problem Statement

Insurance processes some of the most sensitive personal data there is — health information in injury claims, financial accounts, driving and location history, sometimes biometrics — under overlapping privacy laws (GDPR/CCPA-style regimes, health-data rules, insurance-specific privacy regulations) and record-keeping obligations that can *require* retention. Retrofitting classification and erasure into a running platform is extremely costly, and immutable audit/ledger stores make naive deletion impossible. The platform needs privacy built in: classification at the schema level, encryption with per-subject keys enabling crypto-shredding, purpose-based consent, data subject request workflows that respect legal retention, and governed retention and disposal.

## 2. Goals
1. **100% of personal-data fields classified** in schemas (enforced by CI annotation checks).
2. **DSR compliance**: 100% of data subject requests fulfilled or lawfully declined within statutory deadlines.
3. **Retention compliance**: 0 records retained beyond schedule without a hold; 0 records disposed while under hold.
4. **Provable erasure**: crypto-shredding produces verifiable evidence; erased data unrecoverable from primary, replicas, search, analytics, and backups per backup-expiry policy.
5. **Consent integrity**: 0 processing activities requiring consent executed without a valid consent record.

## 3. Non-Goals
- **Legal interpretation** of each regulation (DPO/legal provide the rule content; R01 provides the machinery).
- **Security incident response tooling** (SIEM/SOAR); R01 provides breach-scoping queries and notification support only.
- **Cookie consent banners for marketing sites.**

## 4. Personas & User Stories
- As a **DPO**, I want a data inventory showing which contexts hold which personal data classes, purposes, legal bases, and retention so that I can maintain records of processing.
- As a **customer**, I want to request access to, correction of, or deletion of my data and receive a response within the legal timeframe.
- As a **privacy analyst**, I want DSR fulfilment to compile data from every context automatically, apply exemptions (e.g., open claim, legal hold, statutory retention), and produce a reviewable package.
- As a **records manager**, I want retention schedules by record class and jurisdiction applied automatically, with disposal approval and evidence.
- As an **engineer**, I want annotations to classify fields so that encryption, masking, and telemetry exclusion happen by default.
- As an **AI governance officer**, I want to define which data classes may be sent to which model providers (F07).
- *Edge:* erasure request from a claimant on an open claim → partial fulfilment with lawful retention explanation; erasure scheduled when retention ends.
- *Edge:* legal hold applied after disposal was scheduled → disposal cancelled.

## 5. Domain Model & Invariants
| Entity | Notes |
|---|---|
| **Data Class** | `PUBLIC`, `INTERNAL`, `PII`, `SENSITIVE_PII` (government IDs, DOB+name), `PHI`, `FINANCIAL_ACCOUNT`, `BIOMETRIC`, `LOCATION`, `CHILD_DATA`, `CRIMINAL` (as applicable) |
| **Field Classification** | schema-level annotation per field → data class, purpose tags |
| **Data Subject** | person identity across contexts (subject ID), key ring (per-subject data encryption keys) |
| **Processing Purpose** | e.g., underwriting, claims handling, fraud prevention, marketing, AI processing, analytics; legal basis per jurisdiction |
| **Consent** | subject × purpose × channel × scope, captured-at, source, version of notice text, withdrawn-at |
| **Retention Class** | record type × jurisdiction → retention period trigger (e.g., N years after claim closure / policy expiry), disposal method |
| **Legal Hold** | scope (claim, policy, party, matter), reason, start, release |
| **DSR** | type (`ACCESS`, `CORRECTION`, `DELETION`, `PORTABILITY`, `RESTRICTION`, `OPT_OUT_OF_SALE/SHARING`), subject verification, deadline, state, per-context fulfilment results, exemptions applied |

**DSR states**: `RECEIVED → VERIFYING → (VERIFIED | REJECTED_UNVERIFIED) → COLLECTING → REVIEW → (FULFILLED | PARTIALLY_FULFILLED | DECLINED) → CLOSED`.

**Invariants**
- INV-R01-1: Every persisted field in schemas holding personal data has a classification; unclassified fields fail CI.
- INV-R01-2: `SENSITIVE_PII`, `PHI`, `FINANCIAL_ACCOUNT`, `BIOMETRIC` are encrypted with the subject's key (CRC-PER-009).
- INV-R01-3: Destroying a subject key is performed only by the retention/erasure service after holds and retention checks, with evidence.
- INV-R01-4: Records under legal hold cannot be disposed.
- INV-R01-5: Processing requiring consent checks a valid consent at execution time.

## 6. Requirements
### P0 — day-1 hooks
- **R01-P0-01 Classification annotations & CI check** for Java domain types, DB schemas, event schemas, and API schemas.
- **R01-P0-02 Per-subject key management** (envelope encryption via KMS), blind indexes for search on encrypted fields, crypto-shredding primitive.
- **R01-P0-03 Retention classes** assigned to records/documents at creation (F06, F02, ledger, claims, policy).
- **R01-P0-04 Telemetry masking** library (CRC-SEC-006) and test helpers.
### P0 — workflows
- **R01-P0-05 Consent management**: capture, versioned notice text, withdrawal, purpose checks API (`ConsentPort.check(subject, purpose, channel)`).
- **R01-P0-06 DSR workflow** with identity verification, per-context fulfilment connectors (each context implements `DsrPort`: export/correct/erase-or-explain), exemption rules, deadline tracking, response package.
  *Acceptance:* Given an access request, a package with data from every registered context is produced for review within 5 business days; contexts without a connector fail the DSR readiness check in CI.
- **R01-P0-07 Legal holds** applied/released (manual + from C02 litigation flag) honored by F06, F02, and disposal.
- **R01-P0-08 Retention & disposal engine**: identifies expired records, proposes disposal batches for approval, executes (crypto-shred / delete via owning context API), records evidence.
- **R01-P0-09 Data inventory / records of processing** generated from classifications and context registrations.
- **R01-P0-10 AI data policy**: data class × provider × region allowlist consumed by F07 gateway (CRC-AI-011).
### P1
- **R01-P1-01** Non-production data masking pipeline (irreversible) for approved use (CRC-TST-010).
- **R01-P1-02** Breach scoping query tool (which subjects/data classes affected by a scope).
- **R01-P1-03** Privacy impact assessment registry linked to PRDs/ADRs.
- **R01-P1-04** Consent receipts to customers.
### P2
- **R01-P2-01** Differential-privacy aggregates for analytics sharing.
- **R01-P2-02** Cross-border transfer impact tooling.

## 7. Interfaces & Events
- Ports: `ClassificationRegistry`, `SubjectKeyPort`, `ConsentPort`, `DsrPort` (implemented by each context), `RetentionPort`.
- **Published**: `privacy.consent.granted.v1`, `…withdrawn.v1`, `privacy.dsr.received.v1`, `…fulfilled.v1`, `privacy.legal-hold.applied.v1`, `…released.v1`, `privacy.retention.disposal-approved.v1`, `privacy.subject-key.destroyed.v1`.
- **Consumed**: `claims.claim.litigation-flagged.v1`, `claims.claim.closed.v1`, `policy.term.expired.v1` (retention triggers).

## 8. AI Integration Points
- PII detection assist for redaction (F06) and DSR package review (L2); data policy enforced at the F07 gateway.

## 9. Audit, Security & Compliance
- Every consent, DSR action, hold, disposal, and key destruction audited (F02) with evidence; supports regulator inquiries and certification audits.

## 10. Success Metrics
| Metric | Type | Target (hypothesis) |
|---|---|---|
| Classified personal-data fields | Leading | 100% |
| DSRs within statutory deadline | Lagging | 100% |
| Disposal under hold | Lagging | 0 |
| Records past retention without hold | Lagging | 0 at quarterly check |
| Processing without required consent | Lagging | 0 |

## 11. Open Questions
- [Legal] [Blocking] Retention schedule per record class and launch jurisdiction; which statutes override deletion.
- [Engineering] [Blocking] Key hierarchy and KMS per region; key-per-subject scalability (ADR).
- [Legal] [Non-blocking] Whether AI processing of claim data needs explicit consent in any launch jurisdiction.

## 12. Dependencies & Phasing
- **Day-1 hooks (P0-01…04) ship with the foundation** before DEEP contexts persist personal data.
- DSR, consent, disposal workflows can be developed in parallel thereafter; each context must deliver its `DsrPort` before production.
