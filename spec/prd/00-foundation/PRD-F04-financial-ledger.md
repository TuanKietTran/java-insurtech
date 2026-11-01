# PRD-F04 — Financial Ledger & Money Movement

| Field | Value |
|---|---|
| ID | PRD-F04 |
| Domain | Foundation — Financial Integrity |
| Tier | **DEEP** |
| Status | Draft v0.1 |
| Owner | Finance Platform (TBD) |
| Depends on | F01 (authority, SoD), F02 (audit), F03 (outbox, idempotency), F05 (posting-rule evaluation), R03 (sanctions screening), S02 (payment rails, bank files) |
| Consumed by | C05, B01, B02, V01, R02, A01 |
| Key contract rules | CRC-MON-001…008; CRC-IDEM-003, 005; CRC-PER-007; CRC-SEC-003, 004, 010; CRC-TST-003, 007 |

## 1. Problem Statement

Claims reserves and payments, premium receivables, refunds, recoveries, and commissions are often tracked as mutable balances in operational tables and reconciled to finance at month end by spreadsheet. Retries cause duplicate payments, corrections overwrite history, and nobody can state the true outstanding reserve or receivable at a moment in time. The platform needs a conventional, append-only, double-entry sub-ledger where every financial movement is balanced, idempotent, dimensioned, and reconcilable at any instant — and a single, governed path for money leaving the company.

## 2. Goals

1. **Always balanced**: trial balance nets to zero per tenant × currency at any instant (continuous check, zero exceptions).
2. **Zero duplicate disbursements** attributable to platform retries or double submission.
3. **Reconcilable at any point**: sub-ledger ⇄ operational views (claim financials, billing balances) ⇄ external GL ⇄ bank, with breaks identified within T+1.
4. **Finance owns the mapping**: chart of accounts and posting rules change through configuration, not code.
5. **Time to answer** "what was outstanding reserve / receivable for X as of date D, as known at T" < 5 s.

## 3. Non-Goals

- **Replacing the corporate general ledger / ERP.** The platform is the insurance sub-ledger; it exports summarized journals to the GL of record.
- **Statutory/IFRS 17/GAAP actuarial measurement** (IBNR, discounting, risk adjustment). Out of scope; the ledger supplies case-level actuals to those processes.
- **Treasury and investment accounting.**
- **Blockchain/distributed ledgers.** Conventional append-only store preferred.
- **Card data handling.** Tokenized via PSP (CRC-SEC-010).

## 4. Personas & User Stories

**Finance controller**
- As a finance controller, I want to configure the chart of accounts and posting rules per tenant so that ledger output matches our accounting policy without code changes.
- As a controller, I want to close an accounting period so that no further postings land in it, with late items posted to the current period carrying their original effective date.
- As a controller, I want daily sub-ledger-to-GL and bank reconciliations with exception lists so that month-end close is a review, not an investigation.

**Claims payment operations**
- As a payment ops analyst, I want every payment order's status (approved, screened, released, settled, returned) visible with rail references so that I can answer payee inquiries.
- As payment ops, I want void and reissue to be a single governed action that reverses and re-posts correctly.

**Engineer in a consuming context (claims, billing)**
- As a claims engineer, I want to submit a "reserve changed" or "payment released" business event and have the ledger apply the configured posting rule idempotently so that I never construct journal lines myself.

**Auditor**
- As an external auditor, I want to trace any GL balance to the journal entries, business events, and approvals behind it.

**Edge cases**
- Posting in a currency different from the policy's functional currency.
- A payment returned by the bank 30 days after settlement.
- A posting request arrives for a closed period.
- The same posting request is submitted twice, concurrently.

## 5. Domain Model & Invariants

| Entity | Key attributes |
|---|---|
| **Account** | tenant, code, name, type (`ASSET`, `LIABILITY`, `EQUITY`, `REVENUE`, `EXPENSE`), normal side, sub-ledger (`PREMIUM`, `CLAIMS`, `COMMISSION`, `CASH`, `SUSPENSE`), status, effective period |
| **Posting Rule** | business event type → journal template (account selection by dimensions), version, effective period, lifecycle per CRC-CFG-005 |
| **Journal Entry** | id, tenant, idempotencyKey (permanent unique), accountingDate, effectiveDate, period, source (business event type + id + context), postingRuleVersion, lines, reversalOf?, memo |
| **Journal Line** | account, side (`DEBIT`/`CREDIT`), amount (non-negative `Money`), functional amount + FX rate ref, dimensions (policy, term, transaction, claim, claimLine, coverage, costCategory, LOB, jurisdiction, producer, vendor, payee, billingAccount, invoice, recovery) |
| **Accounting Period** | tenant, start/end, status (`OPEN`, `SOFT_CLOSED`, `CLOSED`) |
| **Balance (projection)** | account × dimension set × currency → balance as of accounting/effective date; rebuildable |
| **Payment Order** | id, idempotencyKey, purpose (`CLAIM_INDEMNITY`, `CLAIM_EXPENSE`, `PREMIUM_REFUND`, `COMMISSION`, `VENDOR`, `RECOVERY_REFUND`), payees + allocation, method (`ACH`, `WIRE`, `CHECK`, `CARD_PUSH`, `VIRTUAL_CARD`), amount, source references, approvals, screening result, rail reference, state |
| **Receipt** | inbound money: source (PSP, lockbox, bank), amount, payer, allocation instructions, state |
| **Reconciliation Run** | type (`SUBLEDGER_GL`, `BANK`, `PSP`, `OPERATIONAL`), period, breaks |

**Payment Order state machine** (CRC-SM-001)

```
REQUESTED ──submit──▶ PENDING_APPROVAL ──approve(all required)──▶ APPROVED ──screen──▶ SCREENED
      │                    │ reject                                   │ hit                 │
      │                    ▼                                          ▼                     │
      │                 REJECTED                                   ON_HOLD ──clear──▶ SCREENED
      │                                                               │ confirm-block
      │                                                               ▼
      │                                                            BLOCKED
SCREENED ──release──▶ RELEASED ──rail ack──▶ SENT ──settle──▶ SETTLED ──return──▶ RETURNED
RELEASED/SENT ──fail──▶ FAILED (retryable or terminal)
APPROVED/SCREENED ──cancel──▶ CANCELLED
SENT/SETTLED ──void/stop──▶ VOIDED      (checks: stop-payment; electronic: recall where possible)
VOIDED/RETURNED ──reissue──▶ (new Payment Order linked via reissueOf)
```
(Auto-approval: when the requester's authority covers the amount and no SoD rule applies, `PENDING_APPROVAL` is passed through with a recorded authority decision.)

**Posting points (indicative; finance approves actual posting rules)**

| Business event | Indicative journal |
|---|---|
| Premium written (bind/endorse) | Dr Premium Receivable / Cr Unearned Premium |
| Premium earned (periodic) | Dr Unearned Premium / Cr Earned Premium |
| Premium receipt | Dr Cash Clearing / Cr Premium Receivable |
| Case reserve increase | Dr Incurred Loss (reserve change) / Cr Case Reserve Liability |
| Claim payment released | Dr Paid Loss / Cr Claim Payments Payable; reserve erosion Dr Case Reserve Liability / Cr Incurred Loss (reserve change) |
| Payment settled | Dr Claim Payments Payable / Cr Bank |
| Recovery received | Dr Cash Clearing / Cr Recoveries |
| Commission earned | Dr Commission Expense / Cr Commission Payable |
| Write-off | Dr Bad Debt Expense / Cr Premium Receivable |

**Invariants**
- INV-F04-1: Σ debits = Σ credits per journal entry per currency (and per functional currency).
- INV-F04-2: Journal entries and lines are immutable; correction only via reversal entries referencing the original.
- INV-F04-3: An idempotency key yields at most one journal entry, ever (permanent unique constraint).
- INV-F04-4: No posting into a `CLOSED` period; late items post to the current open period with original `effectiveDate`.
- INV-F04-5: Case reserve liability balance per claim line × cost category is never negative.
- INV-F04-6: A payment order is `RELEASED` only if approved by principals whose authority covers the amount, SoD satisfied, and screening `CLEAR`.
- INV-F04-7: Operational balances (claim outstanding, billing account balance, commission payable) equal ledger balances for the same dimensions (continuous reconciliation).
- INV-F04-8: Payee bank details used for release are the version approved at approval time; a later change invalidates pending approvals.

## 6. Requirements

### P0 — Must-have

**F04-P0-01 Posting API.** `post(PostingRequest{businessEventType, sourceRef, idempotencyKey, effectiveDate, facts, dimensions})` resolves the posting rule version effective on `effectiveDate`, builds the entry, validates balance and account status, and commits. Callers never send raw journal lines (except authorized manual journals, P0-07).
*Acceptance:*
- Given a posting request already committed, when re-submitted with the same key and payload, then the original entry ID is returned and no new entry exists.
- Given an unbalanced template output, then the request is rejected with `LEDGER_UNBALANCED` and an alert is raised (configuration defect).

**F04-P0-02 Chart of accounts & posting rules as configuration.** Versioned, effective-dated, four-eyes-approved; golden-file tests per rule version (CRC-TST-007); impact preview against sample events before publish.

**F04-P0-03 Reversal & correction.** Reverse an entry fully (new entry with swapped sides, `reversalOf` link); corrections = reversal + new posting, linked by correlation.

**F04-P0-04 Balances & as-of queries.** Balance by account × any dimension subset × currency as of accounting date and effective date; projections rebuildable from journal.

**F04-P0-05 Payment orders.** Lifecycle per state machine above; approval by authority (F01), SoD, sanctions screening (R03) before release, idempotent submission to rails (S02, CRC-IDEM-005), status updates from rail callbacks/files, void/stop/reissue, returns.
*Acceptance:*
- Given a payee with a sanctions hit, the order enters `ON_HOLD`, cannot be released, and a compliance work item is created.
- Given a duplicate release attempt (double click, retry), exactly one rail instruction is sent.
- Given a bank return after settlement, a reversal posting is created and the source context (claims/billing) is notified to reopen the obligation.

**F04-P0-06 Duplicate disbursement detection.** Before approval, flag potential duplicates: same payee + amount + source (claim line / invoice) within a configurable window, or same vendor invoice number. Flags require explicit acknowledgement with reason to proceed.

**F04-P0-07 Manual journals.** Authorized finance users may post manual adjusting entries with mandatory reason, attachment, and second approval; restricted accounts cannot be manually posted.

**F04-P0-08 Accounting periods.** Open/soft-close/close per tenant; closing produces period trial balance snapshot; reopening a closed period is prohibited (adjust in current period).

**F04-P0-09 Reconciliation.**
- Continuous: journal balance ⇄ operational balances (claims outstanding, billing account balances) by dimension.
- Daily: bank statement import (camt.053/BAI2 via S02) and PSP settlement files matched to payment orders and receipts; unmatched items to a breaks queue.
- Periodic: GL export totals ⇄ sub-ledger totals.

**F04-P0-10 GL export.** Summarized journal export per period by GL account mapping (file or API via S02), idempotent and re-runnable with the same output.

**F04-P0-11 Receipts & suspense.** Inbound receipts post to cash clearing; unidentified receipts go to suspense with a work item; allocation is delegated to the owning context (B01) and posts accordingly.

**F04-P0-12 Multi-currency.** Transaction and functional currency on every line; FX rate snapshot with source (CRC-MON-007); revaluation is P1.

### P1 — Fast follow
- **F04-P1-01** FX revaluation of open monetary balances at period end.
- **F04-P1-02** Positive pay file generation for checks; escheatment workflow for stale-dated checks (dormancy rules by jurisdiction).
- **F04-P1-03** Payment batching and scheduling windows per rail; cut-off management.
- **F04-P1-04** Tax reporting data capture and year-end information return extracts for payees (where required, e.g., US 1099).
- **F04-P1-05** Ledger explorer UI: drill from balance → entries → business event → audit timeline.

### P2 — Future
- **F04-P2-01** Reinsurance ceded sub-ledger (ceded premium, ceded loss, recoverables).
- **F04-P2-02** Real-time payment rails with request-to-pay.
- **F04-P2-03** Multi-entity intercompany postings.

## 7. Interfaces & Events

**APIs**: `POST /v1/postings` (internal), `GET /v1/balances`, `POST /v1/payment-orders`, `POST /v1/payment-orders/{id}/approve|release|void|reissue`, `GET /v1/payment-orders/{id}`, `POST /v1/manual-journals`, `POST /v1/periods/{id}/close`.

**Events published**: `ledger.journal-entry.posted.v1`, `ledger.journal-entry.reversed.v1`, `ledger.payment-order.requested.v1`, `…approved.v1`, `…on-hold.v1`, `…released.v1`, `…settled.v1`, `…failed.v1`, `…returned.v1`, `…voided.v1`, `ledger.receipt.recorded.v1`, `ledger.period.closed.v1`, `ledger.reconciliation.break-detected.v1`.

**Events consumed**: `claims.reserve.changed.v1`, `claims.payment.approved.v1`, `claims.recovery.received.v1`, `billing.premium.written.v1`, `billing.premium.earned.v1`, `billing.payment.allocated.v1`, `billing.refund.approved.v1`, `billing.write-off.approved.v1`, `commissions.commission.earned.v1`, `commissions.payout.approved.v1`, `fraud.screening.completed.v1`.

## 8. AI Integration Points
- AI Agents have **no** posting or payment tools (CRC-AI-002).
- P1: reconciliation break explanation (AI, L2 assist) summarizing likely causes from related events, with citations; humans resolve.

## 9. Audit, Security & Compliance
- Every posting links to its source business event and audit chain; every payment order links to approvals and authority decisions.
- Payee bank detail changes: SoD, step-up auth (F01-P1-03), out-of-band verification for vendors (V01), cooling period configurable.
- Supports SOX-style controls evidence (four-eyes, period close, reconciliations), prompt-payment regulation evidence (timestamps per state).

## 10. Success Metrics

| Metric | Type | Target (hypothesis) |
|---|---|---|
| Unbalanced entries committed | Leading | 0 (impossible by construction; monitored) |
| Duplicate disbursements | Lagging | 0 |
| Reconciliation breaks aged > T+1 | Lagging | < 0.5% of daily items |
| Operational ⇄ ledger variance | Leading | 0 (continuous check) |
| Approved → released latency p95 (electronic) | Leading | < 15 min (excluding batch windows) |
| Month-end close: sub-ledger tie-out effort | Lagging | ≤ 1 day |

## 11. Open Questions
- [Finance] [Blocking] Chart of accounts and posting policy per tenant: statutory vs. GAAP basis in sub-ledger, or basis-neutral with mapping at export?
- [Finance] [Blocking] When is a claim payment recognized: at approval, release, or settlement? (Indicative table uses release.)
- [Engineering] [Blocking] Ledger store: dedicated Postgres schema with partitioning vs. specialized ledger database — ADR with throughput targets.
- [Treasury] [Non-blocking] Rails and banks per market for launch; check printing vendor.
- [Legal] [Non-blocking] Escheatment obligations per jurisdiction (P1).

## 12. Dependencies & Phasing
- **First**: posting API, accounts/posting-rule config, periods, balances, reversal — needed before C05 reserves and B01 premium can post.
- **Then**: payment orders with approval/SoD/screening, rails via S02, bank reconciliation — needed before first real disbursement.
- **Parallel**: GL export, explorer UI, positive pay, escheatment, FX revaluation.
