# PRD-B01 — Premium Billing & Collections

| Field | Value |
|---|---|
| ID | PRD-B01 |
| Domain | Billing & Payments |
| Tier | **PARALLEL** (financial correctness guaranteed by DEEP F04) |
| Status | Draft v0.1 |
| Owner | Billing (TBD) |
| Depends on | F04 (posting, receipts, payment orders, reconciliation), P02 (premium facts, cancellation/reinstatement), F05 (allocation, delinquency, notice rules), S02 (PSP, bank), X04 (invoices, notices), F01, F02, F03 |
| Consumed by | P02 (nonpayment cancellation, renewal acceptance), B02 (collected premium), X01, X02, A01, R03 |
| Key contract rules | CRC-MON-*; CRC-IDEM-*; CRC-SEC-010; CRC-SM-008; CRC-CFG-002 |

## 1. Problem Statement

Premium that is billed incorrectly, allocated to the wrong installment, or chased with the wrong notice periods creates customer complaints, unjustified cancellations, regulatory exposure, and receivables nobody trusts. Billing must translate every policy transaction into the right schedule of invoices, take payments through modern rails without handling card data, run delinquency exactly per jurisdiction rules, and refund return premium promptly — all reconcilable to the ledger.

## 2. Goals

1. **Billing accuracy**: billed amounts equal written premium + fees/taxes per term exactly (0 unexplained variance).
2. **Correct delinquency**: 100% of nonpayment cancellations follow configured grace and notice rules; 0 wrongful cancellations caused by misallocation.
3. **Collection performance**: autopay enrollment ≥ 60% of eligible accounts (hypothesis); DSO baseline then −10%.
4. **Prompt refunds**: return premium refunded within 5 business days of cancellation for ≥ 95% (or jurisdiction requirement if stricter).
5. **Daily reconciliation**: PSP/bank receipts reconciled to billing allocations with breaks < 0.5% aged > T+1.

## 3. Non-Goals

- **Card data storage** (PSP tokenization only, CRC-SEC-010).
- **Premium finance company operations** (P2 integration).
- **Commission calculation** (B02).
- **Corporate AR outside insurance premium.**

## 4. Personas & User Stories

- As a **policyholder**, I want to choose pay-in-full or monthly installments, set up autopay, and see what I owe and when.
- As a **policyholder**, I want a grace period reminder before my policy is at risk, and immediate reinstatement if I pay within the allowed window.
- As a **billing specialist**, I want to move a misapplied payment, waive a fee within my authority, or set up a payment arrangement with an audit trail.
- As a **producer (agency bill)**, I want an agency statement of premiums due net of commission so that I can remit in one payment.
- As a **finance analyst**, I want unidentified receipts in a suspense queue and daily PSP reconciliation.
- *Edge:* mid-term endorsement increases premium → remaining installments re-spread (or next invoice catch-up) per billing plan rules.
- *Edge:* ACH payment returned after the grace period → delinquency resumes with correct dates and notices.
- *Edge:* refund owed but account has another policy with a balance → apply per offset rules, then refund remainder.

## 5. Domain Model & Invariants

| Entity | Notes |
|---|---|
| **Billing Account** | payer (party), bill type (`DIRECT`, `AGENCY`), policy terms linked, payment methods (PSP tokens), autopay settings, balance (projection), delinquency state |
| **Billing Plan** | config: installments count, down payment %, fees (installment, late, NSF), invoice lead days, due-day rules, re-spread method |
| **Schedule & Charges** | charges per term from premium facts (by coverage/installment), fees, taxes; schedule of installments |
| **Invoice** | account, period, lines (charges), due date, amount due, status |
| **Receipt Allocation** | receipt (F04) → allocations to invoices/charges per allocation rules (F05) |
| **Credit / Refund** | return premium, overpayments; refund via F04 payment order |
| **Write-off / Adjustment** | reason, authority, amount |
| **Payment Arrangement** | promise-to-pay schedule overriding standard delinquency (authority) |

**Invoice states**: `PLANNED → ISSUED → (PAID | PARTIALLY_PAID → PAID) | PAST_DUE → (PAID | WRITTEN_OFF | CANCELLED)`.

**Delinquency state machine (per account × term)**: `CURRENT → GRACE → NOTICE_OF_INTENT_SENT → CANCELLATION_REQUESTED (→ P02 PENDING_CANCELLATION) → CANCELLED_FOR_NONPAYMENT → (REINSTATEMENT_ELIGIBLE → CURRENT on cure)`; any pre-cancel state → `CURRENT` on cure. Timers per CRC-SM-008; durations and notice requirements per jurisdiction (F05).

**Invariants**
- INV-B01-1: Σ scheduled charges for a term = written premium + fees + taxes for that term (exact allocation).
- INV-B01-2: Every allocation, adjustment, write-off, and refund posts to F04 via posting rules exactly once (idempotent).
- INV-B01-3: Billing account balance projection = ledger receivable balance for the account dimensions.
- INV-B01-4: Delinquency cannot request cancellation before required notice periods have elapsed with proof of notice (X04).
- INV-B01-5: Refunds and write-offs above thresholds require authority/approval (F01).

## 6. Requirements

### P0 — Must-have
- **B01-P0-01 Billing account creation** on bind (saga from P02) with plan selection and payment method tokenization via PSP (S02).
- **B01-P0-02 Charges & schedules** from `policy.transaction.applied.v1` premium facts; re-spread or catch-up per plan for endorsements; cancellation credits.
- **B01-P0-03 Invoicing** with lead time, due-day rules, delivery via X04 (email/print/portal) and document generation (F06).
- **B01-P0-04 Payments**: card, ACH/direct debit, wallet; one-time and autopay; partial payments; webhooks/files from PSP; NSF/chargeback handling with reversal postings.
- **B01-P0-05 Allocation rules** (F05): oldest-first, fee priority, policy priority across multi-policy accounts; manual reallocation with reason.
- **B01-P0-06 Delinquency** per state machine with jurisdiction rules, notices with proof of mailing, cancellation request to P02, rescission on cure, reinstatement eligibility.
  *Acceptance:* Given payment received before the cancellation effective date, P02 rescinds the pending cancellation and the delinquency returns to `CURRENT`, with notices of rescission sent where required.
- **B01-P0-07 Refunds** of return premium and overpayments via F04 payment orders to original method where possible; offset rules.
- **B01-P0-08 Adjustments & write-offs** (small balance thresholds auto, above with authority).
- **B01-P0-09 Earning** of premium (daily/monthly per finance policy) emitted as `billing.premium.earned.v1` for F04 posting (or delegated to F04 earning job — open question).
- **B01-P0-10 Agency bill**: agency statements net of commission (B02), remittance matching, producer balance tracking.
- **B01-P0-11 Reconciliation** with F04: receipts, allocations, PSP settlements; breaks queue.

### P1 — Fast follow
- **B01-P1-01** Payment arrangements and hardship plans.
- **B01-P1-02** Smart retry for failed autopay (timing rules).
- **B01-P1-03** Account consolidation (one bill, multiple policies) and paperless incentives.
- **B01-P1-04** Pay-by-link and IVR payments.

### P2 — Future
- **B01-P2-01** Premium finance integrations.
- **B01-P2-02** Usage-based billing (variable monthly premium).
- **B01-P2-03** Multi-currency billing for international programs.

## 7. Interfaces & Events
- APIs: `/v1/billing-accounts`, `/v1/billing-accounts/{id}/invoices`, `/v1/billing-accounts/{id}/payments` (initiate), `/v1/billing-accounts/{id}/autopay`, `/v1/refunds`, `/v1/adjustments`, `/v1/agency-statements`.
- **Events published**: `billing.account.created.v1`, `billing.premium.written.v1`, `billing.premium.earned.v1`, `billing.invoice.issued.v1`, `billing.payment.received.v1`, `billing.payment.allocated.v1`, `billing.payment.reversed.v1`, `billing.account.delinquent.v1`, `billing.account.cured.v1`, `billing.refund.approved.v1`, `billing.write-off.approved.v1`.
- **Events consumed**: `policy.term.bound.v1`, `policy.transaction.applied.v1`, `policy.term.cancelled.v1`, `policy.term.reinstated.v1`, `policy.term.renewal-offered.v1`, `ledger.receipt.recorded.v1`, `ledger.payment-order.settled.v1`/`…returned.v1`.

## 8. AI Integration Points
- AI06 drafts billing correspondence (non-required notices) (L3). Required notices use fixed templates.
- P1: payment-risk propensity as an input to reminder timing (rules decide; fairness reviewed).

## 9. Audit, Security & Compliance
- PCI scope minimized via PSP tokenization; bank details encrypted (CRC-PER-009).
- Notice proof retained (F06); delinquency decisions audited with rule versions.
- R03 AML signals: overpayment + refund-to-different-instrument patterns.

## 10. Success Metrics

| Metric | Type | Target (hypothesis) |
|---|---|---|
| Billing ⇄ written premium variance | Leading | 0 |
| Wrongful nonpayment cancellations | Lagging | 0 |
| Autopay enrollment | Lagging | ≥ 60% eligible |
| Refund within 5 business days | Leading | ≥ 95% |
| Recon breaks aged > T+1 | Leading | < 0.5% |

## 11. Open Questions
- [Finance] [Blocking] Earning method/frequency and which context owns the earning job (B01 vs. F04).
- [Legal] [Blocking] Grace/notice periods and fee rules per launch jurisdiction.
- [Treasury] [Non-blocking] PSP selection and supported methods per market.

## 12. Dependencies & Phasing
- Needs F04 posting/receipts and P02 premium facts contracts; can build against stubs. Payment UI (X01/X03) and agency statements (X02) in parallel.
