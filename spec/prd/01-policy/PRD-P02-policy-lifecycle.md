# PRD-P02 — Policy Lifecycle & Versioning

| Field | Value |
|---|---|
| ID | PRD-P02 |
| Domain | Policy |
| Tier | **DEEP** |
| Status | Draft v0.1 |
| Owner | Policy Platform (TBD) |
| Depends on | P01 (product versions), P03 (rating), U01 (referrals), F05 (eligibility/notice rules), F06 (document generation), F03 (sagas, timers), F02, F01 |
| Consumed by | C01, C02, C04 (CoverageSnapshot), B01, B02, X01, X02, R02, A01, AI03, AI07 |
| Key contract rules | CRC-SM-001…010; CRC-TIME-003, 004, 007; CRC-IDEM-*; CRC-MON-004; CRC-EVT-* |

## 1. Problem Statement

A policy is a contract that changes over time: quoted, bound, issued, endorsed mid-term (sometimes back-dated), renewed, cancelled, and reinstated. Claims need to know exactly what coverage was in force at the instant of loss *as the insurer knew it then and as corrected since*. Systems that overwrite policy records, or that cannot process out-of-sequence changes, produce wrong coverage decisions, premium errors, and billing disputes. The platform needs an explicit lifecycle with bitemporal versioning and a clean coverage-snapshot contract for claims.

## 2. Goals

1. **Correct coverage as of any instant**: `getCoverageAsOf(policy, lossInstant, knownAt)` returns the correct snapshot for 100% of golden scenarios, including OOSE, cancellation, and reinstatement cases.
2. **Premium integrity**: written premium on the term always equals the sum of transaction premiums; 100% of transactions post to billing/ledger exactly once.
3. **Straight-through servicing**: ≥ 70% of personal-lines endorsements complete without manual intervention (hypothesis; depends on product rules).
4. **Regulatory notice compliance**: 100% of cancellations/non-renewals honor configured notice periods and proof-of-mailing requirements.
5. **Quote performance**: quote-to-price p95 < 2 s end-to-end for personal lines (including rating).

## 3. Non-Goals

- **Rating math** (P03) and **UW decisioning** (U01); P02 orchestrates them.
- **Billing schedules and collections** (B01); P02 emits premium facts.
- **Premium audit for commercial lines** (P2).
- **Policy administration migration tooling** for legacy books (separate initiative; P02 must expose bulk import of in-force versions).

## 4. Personas & User Stories

**Producer / CSR**
- As a producer, I want to quote, adjust options, and bind in one flow within my binding authority so that I can close the sale on the call.
- As a CSR, I want to process an endorsement (add a vehicle effective last Tuesday) and see the premium change and its pro-rata calculation before applying it.

**Policyholder**
- As a policyholder, I want to request simple changes (address, add driver) and receive updated documents immediately.

**Underwriter**
- As an underwriter, I want referred quotes and endorsements to stop at my queue with the referral reasons, and resume automatically after I approve.

**Policy operations**
- As policy ops, I want renewals generated N days before expiry with re-rating and re-underwriting so that offers go out on time.
- As policy ops, I want nonpayment cancellations to be scheduled with jurisdiction-correct notice and rescinded automatically if payment arrives before the effective date.

**Claims (consumer)**
- As an adjuster, I want to see the coverages, limits, deductibles, forms, and risk objects that applied at the loss time, plus a note if the policy changed after the loss.

**Edge cases**
- OOSE: an endorsement back-dated before an already-applied later endorsement.
- Loss date during a lapse between cancellation and reinstatement-with-lapse.
- Cancellation flat (from inception) after a claim was reported.
- Renewal offer accepted after expiry within a grace window.

## 5. Domain Model & Invariants

| Entity | Notes |
|---|---|
| **Quote** | product version, applicant, risk objects, selected coverages/options, rating result ref, UW status, validity period |
| **Policy** | policy number, tenant, product, producer of record, named insured(s) — durable across terms |
| **Policy Term** | term period `[effectiveFrom, effectiveTo)` with effective-time convention, status, renewal-of link |
| **Policy Transaction** | type (`NEW_BUSINESS`, `ENDORSEMENT`, `CANCELLATION`, `REINSTATEMENT`, `RENEWAL`, `REWRITE`), effective date, requested changes, rating result, premium delta, documents, status |
| **Policy Version** | immutable state after a transaction: coverages, limits, deductibles, risk objects, parties, forms, product version; valid-time interval + recordedAt (bitemporal) |
| **Coverage Snapshot** | read model: derived from policy versions for (term, effective instant, knownAt); content hash; ID referenced by claims |

**Quote states**: `DRAFT → RATED → (REFERRED → RATED | DECLINED) → QUOTED → BOUND` ; `QUOTED → EXPIRED | WITHDRAWN`.

**Policy Term states**

```
          bind                 issue
QUOTE ──────────▶ BOUND ───────────────▶ ISSUED ─────────── term end ──────────▶ EXPIRED
                    │                      │  ▲
                    │ cancel (flat)        │  │ rescind / reinstate
                    ▼                      ▼  │
                CANCELLED ◀── effective ── PENDING_CANCELLATION
                    │
                    └── reinstate (with or without lapse) ──▶ ISSUED
Renewal: new term created as RENEWAL_OFFERED ──accept/pay──▶ BOUND ──▶ ISSUED
                                   └── not accepted by expiry (+grace) ──▶ LAPSED
                                   └── non-renewal decision ──▶ NON_RENEWED
```
Temporal status (`PENDING_EFFECTIVE`, `IN_FORCE`) is derived from dates, not stored as lifecycle state (CRC-SM-010).

**Policy Transaction states**: `DRAFT → RATED → (REFERRED → APPROVED | DECLINED) → QUOTED → APPLIED`; `→ WITHDRAWN`. Only `APPLIED` produces a Policy Version.

**Invariants**
- INV-P02-1: Policy versions never change; corrections are new transactions. Each version has a valid-time interval and recordedAt.
- INV-P02-2: For any (term, effective instant, knownAt) exactly one policy version applies, or none (not in force).
- INV-P02-3: Term written premium = Σ transaction premium deltas (exact, CRC-MON-004).
- INV-P02-4: A term in `CANCELLED`, `EXPIRED`, `LAPSED`, or `NON_RENEWED` accepts no endorsements (only reinstatement where configured, or corrective transactions with authority).
- INV-P02-5: Binding requires: quote `QUOTED` and valid, UW approval if referred, eligibility pass on the bind date, billing plan selected, binding authority of the binder.
- INV-P02-6: OOSE processing re-applies later transactions on top of the back-dated change; conflicts (same attribute changed later) require explicit resolution; resulting premium deltas are recomputed and posted as adjustments.
- INV-P02-7: Cancellation effective date respects notice rules for the reason and jurisdiction (F05), unless insured-requested.
- INV-P02-8: Policy number and term identity never change; rewrite creates a new term (and optionally new policy number) linked to the old.

## 6. Requirements

### P0 — Must-have

**P02-P0-01 Quote.** Create/update quotes against a resolved product version; validate risk objects against schemas; call rating (P03) and eligibility/referral (F05/U01); multiple option sets per quote; validity period; quote documents.
*Acceptance:* Given a quote that triggers a referral rule, the quote enters `REFERRED` with reasons; after UW approval it returns to `RATED` and can be quoted; declines produce reason codes for adverse-action notices (X04).

**P02-P0-02 Bind.** Idempotent bind command enforcing INV-P02-5; creates Policy, Term, NB transaction, first Policy Version; triggers bind saga: billing account/plan (B01), commission (B02), document generation (F06), notifications (X04).
*Acceptance:* replaying the same bind request returns the same policy number and creates no duplicate term or billing account.

**P02-P0-03 Issue.** Policy number allocation (configurable format per product), document packet generation (declarations, forms per schedule), delivery (X04). Binder documents available before issuance where required.

**P02-P0-04 Endorsements.** Mid-term changes with effective date, pro-rata (or configured method) premium delta, referral if triggered, preview before apply; endorsement documents.

**P02-P0-05 Out-of-sequence endorsements.** Back-dated changes re-sequence later transactions automatically where non-conflicting; conflicting attributes require user resolution; premium recomputation for all affected periods.
*Acceptance:* golden scenario suite (≥ 30 cases) of OOSE combinations produces expected versions and premiums.

**P02-P0-06 Cancellation.** Reasons (insured request, nonpayment, UW, fraud/misrepresentation, rewrite), methods (flat, pro-rata, short-rate per configuration), notice periods and mailing requirements by jurisdiction and reason (F05), `PENDING_CANCELLATION` with scheduled effective command (CRC-SM-008), rescission on cure (e.g., payment received), return premium calculation → B01 refund.

**P02-P0-07 Reinstatement.** With or without lapse; conditions by reason (e.g., payment within window, UW approval); lapse period recorded so coverage snapshots show no coverage during lapse.

**P02-P0-08 Renewal.** Scheduled renewal generation N days before expiry (configurable by product/jurisdiction), product version resolution for renewal date, re-rate, re-underwrite (referral if triggered), offer documents, acceptance by payment or explicit action, lapse at expiry + grace; non-renewal with notice rules.

**P02-P0-09 Bitemporal versions & as-of queries.** `getPolicyAsOf(term, effectiveInstant, knownAt)` and `getCoverageAsOf(policyRef, lossDateTime, knownAt)` returning an immutable CoverageSnapshot (ID + hash) including coverages, limits (with aggregate definitions), deductibles, risk objects, parties, form editions, product version.
*Acceptance:* Given a loss date during a lapse, the snapshot API returns `NOT_IN_FORCE` with the reason; given a later back-dated endorsement, querying with an earlier `knownAt` returns the pre-endorsement snapshot.

**P02-P0-10 Policy search & lookup** for FNOL: by policy number, insured name + address/DOB, VIN/plate, property address (via S01), with permission filtering.

**P02-P0-11 Premium facts.** Each applied transaction emits written premium delta by coverage, taxes/fees, with effective periods for earning (consumed by B01/F04).

### P1 — Fast follow
- **P02-P1-01** Rewrite and policy-change of product version mid-term (rewrite flow).
- **P02-P1-02** Bulk renewals monitoring dashboard and exception queue.
- **P02-P1-03** Quote comparison and proposal documents for producers.
- **P02-P1-04** Bulk import of in-force policies for migration with validation reports.
- **P02-P1-05** Self-service endorsement rules (which changes customers can apply without review).

### P2 — Future
- **P02-P2-01** Premium audit (commercial exposure true-up).
- **P02-P2-02** Master policies / certificates (group, embedded insurance).
- **P02-P2-03** Usage-based policies with continuous exposure feeds.

## 7. Interfaces & Events
- APIs: `/v1/quotes`, `/v1/quotes/{id}/rate|submit-referral|bind`, `/v1/policy-terms/{id}/endorsements`, `…/cancellations`, `…/reinstatements`, `…/renewals`, `GET /v1/policies/{id}/as-of?effective=&knownAt=`, `GET /v1/coverage-snapshots?policyRef=&lossDateTime=&knownAt=`, `GET /v1/coverage-snapshots/{id}`.
- **Events published**: `policy.quote.rated.v1`, `policy.quote.referred.v1`, `policy.quote.declined.v1`, `policy.term.bound.v1`, `policy.term.issued.v1`, `policy.transaction.applied.v1` (with premium delta), `policy.term.cancellation-scheduled.v1`, `policy.term.cancellation-rescinded.v1`, `policy.term.cancelled.v1`, `policy.term.reinstated.v1`, `policy.term.renewal-offered.v1`, `policy.term.renewed.v1`, `policy.term.non-renewed.v1`, `policy.term.lapsed.v1`, `policy.term.expired.v1`, `policy.version.retroactively-changed.v1` (signals claims to re-evaluate affected snapshots).
- **Events consumed**: `underwriting.referral.decided.v1`, `billing.account.delinquent.v1` (nonpayment cancellation), `billing.payment.received.v1` (rescission/reinstatement/renewal acceptance).

## 8. AI Integration Points
- AI07: plain-language explanation of policy changes and endorsement suggestions to producers (L2).
- AI02: extraction from submitted applications/supporting docs into quote fields (L3 proposal).
- AI never binds, declines, cancels, or non-renews (CRC-AI-002).

## 9. Audit, Security & Compliance
- Every transaction audited with before/after version, rating worksheet ref, referral decisions, notices sent with proof.
- Notice timing evidence for regulatory exams; adverse-action reason codes retained.
- Producer binding authority and licensing (F01) enforced at bind.

## 10. Success Metrics

| Metric | Type | Target (hypothesis) |
|---|---|---|
| CoverageSnapshot golden-scenario correctness | Leading | 100% |
| Premium mismatch (term vs. Σ transactions vs. ledger) | Lagging | 0 |
| Endorsement straight-through rate (personal lines) | Lagging | ≥ 70% |
| Notices sent within required window | Lagging | 100% |
| Quote p95 latency | Leading | < 2 s |
| Renewal offers generated on schedule | Leading | ≥ 99.5% |

## 11. Open Questions
- [Product] [Blocking] Cancellation methods and notice rules per launch jurisdiction (rule content for F05).
- [Engineering] [Blocking] Storage of bitemporal versions: full snapshots vs. delta + materialized snapshots — ADR (snapshot reads are a claims hot path).
- [Claims] [Non-blocking] When a retroactive policy change affects an open claim's snapshot, auto re-evaluate coverage or create a review task? (Default: review task.)
- [Finance] [Non-blocking] Short-rate tables per jurisdiction ownership (product vs. finance).

## 12. Dependencies & Phasing
- **First**: Term/Transaction/Version model, bitemporal as-of, CoverageSnapshot API (unblocks C04), bind/issue with saga.
- **Then**: endorsements incl. OOSE, cancellation/reinstatement with timers and notices, renewals.
- **Parallel**: producer/customer UIs (X01/X02), migration import, quote proposal documents.
