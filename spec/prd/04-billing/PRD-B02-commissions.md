# PRD-B02 — Commissions & Producer Compensation

| Field | Value |
|---|---|
| ID | PRD-B02 |
| Domain | Billing & Payments |
| Tier | **PARALLEL** |
| Status | Draft v0.1 |
| Owner | Billing (TBD) |
| Depends on | F04 (posting, payment orders), P02 (premium facts, producer of record), B01 (collected premium, agency bill netting), F01 (producer hierarchy, licenses/appointments), F05 (commission rules), S02 (licensing data) |
| Consumed by | X02 (statements), A01 (distribution economics), R02 |
| Key contract rules | CRC-MON-004; CRC-IDEM-003; CRC-CFG-001; CRC-SEC-003 |

## 1. Problem Statement

Producers are paid on complex plans — new vs. renewal rates, tiers, splits across agency hierarchies, chargebacks on cancellation — and errors erode trust in the carrier quickly. Paying commission to an unlicensed or unappointed producer is a regulatory violation. The platform needs configurable commission plans computed from premium events, with hierarchy splits, chargebacks, statements, and payouts through the ledger.

## 2. Goals

1. **Accuracy**: commission disputes per 1,000 statements < 2 (hypothesis); 0 unexplained statement variances.
2. **Compliance**: 0 commissions paid to producers lacking a valid license/appointment for the jurisdiction on the transaction date.
3. **Timeliness**: statements and payouts produced on schedule ≥ 99.5%.
4. **Configurable**: new commission plans and rate changes without code.

## 3. Non-Goals
- **Contingent/profit-sharing commission calculation** (P2).
- **Producer onboarding/licensing system of record** (sourced via S02; F01 attributes).
- **Sales performance management/incentive gamification.**

## 4. Personas & User Stories
- As a **distribution manager**, I want to define commission plans by product, jurisdiction, NB/renewal, and producer tier, effective-dated.
- As an **agency principal**, I want monthly statements showing each policy's premium, rate, commission, splits, and chargebacks.
- As a **finance analyst**, I want commission payable reconciled to the ledger and payouts batched through F04.
- *Edge:* cancellation return premium → proportional commission chargeback, netted against future commission or billed.
- *Edge:* producer of record change mid-term → split by effective date.

## 5. Domain Model & Invariants
| Entity | Notes |
|---|---|
| **Commission Plan** | rules (rate tables by product/jurisdiction/transaction type/tier), basis (`WRITTEN` or `COLLECTED`), split rules for hierarchy, version/effective dates |
| **Commission Entry** | policy transaction or collection ref, producer, basis amount, rate, amount, type (`EARNED`, `CHARGEBACK`, `ADJUSTMENT`), plan version |
| **Producer Balance** | payable/receivable per producer (projection = ledger) |
| **Statement** | period, producer/agency, entries, totals, document (F06) |
| **Payout** | statement ref, F04 payment order, netting with agency-bill remittance |

**Invariants**
- INV-B02-1: Entries derive deterministically from premium/collection events + plan version; recomputation reproduces them.
- INV-B02-2: Split amounts sum exactly to the commission (CRC-MON-004).
- INV-B02-3: License/appointment validity checked for producer × jurisdiction × transaction date; failing entries are held, not paid.
- INV-B02-4: Every entry posts once to F04 (permanent idempotency key per source event × producer).

## 6. Requirements
### P0
- **B02-P0-01** Commission plans as effective-dated configuration (F05 tables).
- **B02-P0-02** Calculation from `policy.transaction.applied.v1` (written basis) or `billing.payment.allocated.v1` (collected basis).
- **B02-P0-03** Hierarchy splits and producer-of-record changes.
- **B02-P0-04** Chargebacks on cancellations/return premium.
- **B02-P0-05** License/appointment gate with hold queue.
- **B02-P0-06** Statements (monthly/configurable) delivered via X02/X04.
- **B02-P0-07** Payouts via F04 payment orders; agency-bill netting with B01.
- **B02-P0-08** Manual adjustments with authority and reason.
### P1
- **B02-P1-01** Advances and draws with recovery schedules.
- **B02-P1-02** Commission simulation for proposed plan changes.
- **B02-P1-03** Year-end tax information for producers where required.
### P2
- **B02-P2-01** Contingent/profit-sharing commission based on loss ratio.
- **B02-P2-02** MGA override and fronting fee arrangements.

## 7. Interfaces & Events
- APIs: `/v1/commission-plans`, `/v1/producers/{id}/commission-entries`, `/v1/commission-statements`, `/v1/commission-adjustments`.
- **Published**: `commissions.commission.earned.v1`, `commissions.chargeback.created.v1`, `commissions.entry.held.v1`, `commissions.statement.issued.v1`, `commissions.payout.approved.v1`.
- **Consumed**: `policy.transaction.applied.v1`, `billing.payment.allocated.v1`, `policy.term.cancelled.v1`, `identity.principal.*` (producer attributes).

## 8. AI Integration Points
- P1: statement Q&A for producers (AI03 pattern, grounded on statement data, L2).

## 9. Audit, Security & Compliance
- Plan changes and manual adjustments audited; license gate decisions recorded for regulatory review.

## 10. Success Metrics
| Metric | Type | Target (hypothesis) |
|---|---|---|
| Disputes per 1,000 statements | Lagging | < 2 |
| Commissions paid without valid license/appointment | Lagging | 0 |
| On-time statements | Leading | ≥ 99.5% |
| Commission payable ⇄ ledger variance | Leading | 0 |

## 11. Open Questions
- [Distribution] [Blocking] Written vs. collected basis per product; hierarchy split defaults.
- [Legal] [Non-blocking] Treatment of held commissions when a license is later found valid retroactively.

## 12. Dependencies & Phasing
- Needs P02 premium facts and F04 posting; can start with written basis; collected basis once B01 allocations exist.
