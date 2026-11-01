# PRD-C05 — Claim Financials: Reserves, Settlement, Payment & Recovery

| Field | Value |
|---|---|
| ID | PRD-C05 |
| Domain | Claims — Financial Integrity |
| Tier | **DEEP** |
| Status | Draft v0.1 |
| Owner | Claims Financials (TBD) |
| Depends on | F04 (ledger, payment orders), F01 (authority, SoD), C02 (lifecycle guards), C03 (approval routing), C04 (payable amounts, limits, deductibles, liability), V01 (vendor invoices), R03 (sanctions, fraud holds), F05 (initial reserve rules), F02, F03 |
| Consumed by | C02, A01 (leakage, reserve adequacy), R02 (loss data), X01/X03 (payment status), B01 (deductible billing, optional) |
| Key contract rules | CRC-MON-*; CRC-IDEM-003, 005; CRC-SEC-003, 004; CRC-SM-*; CRC-TST-003; CRC-AI-002 |

## 1. Problem Statement

Reserves tell the company what it owes; payments discharge it; recoveries claw some back. When these are maintained as editable numbers, outstanding reserves go stale or negative, payments exceed authority or limits, duplicate payments slip through, salvage and subrogation are missed, and finance cannot reconcile claims to the ledger. The platform needs a claim financial model where every reserve change, payment, and recovery is an idempotent, authorized, audited movement posted to the double-entry ledger, with incurred always explainable and reconcilable.

## 2. Goals

1. **Reconcilable**: claim-level paid/outstanding/recovered equals ledger balances by dimension at all times (0 variance).
2. **No unauthorized money**: 100% of reserve changes and payments within actor authority or approved by someone with sufficient authority; SoD enforced.
3. **No duplicate payments** (platform-caused) and duplicate-detection catch rate ≥ 95% on seeded test cases.
4. **Reserve adequacy**: initial reserve accuracy (initial vs. ultimate on closed claims, per segment) improves 20% vs. baseline within 12 months.
5. **Recovery capture**: subrogation identification rate on eligible claims ≥ 90% (QA sample) and salvage recovery tracked on 100% of total losses.

## 3. Non-Goals

- **IBNR and actuarial reserving** (aggregate methods). C05 handles case reserves only.
- **Bill review engines** for medical bills (via S02/V01 integration).
- **Structured settlement annuity purchasing** (P2).
- **Reinsurance recoveries** (P2, F04-P2-01).

## 4. Personas & User Stories

**Adjuster**
- As an adjuster, I want to set an initial reserve suggested by rules for my segment and adjust it with a reason as facts emerge so that reserves reflect expected cost.
- As an adjuster, I want to issue a payment to multiple payees (insured + lienholder) with the deductible applied automatically so that the check is right the first time.
- As an adjuster, I want to know before submitting if a payment would exceed my authority, the outstanding reserve, or the remaining limit.

**Claims supervisor / approver**
- As an approver, I want approval requests that show the reserve history, coverage position, limit availability, duplicate warnings, and fraud signals in one view.

**Recovery specialist**
- As a subrogation specialist, I want claims with recovery potential flagged with liability evidence so that I can open recovery, send demands, and track receipts.
- As a salvage coordinator, I want total-loss vehicles assigned to salvage vendors with title tracking and proceeds posted automatically.

**Finance / actuarial**
- As an actuary, I want reserve change history with reasons and timing by segment so that I can analyze case reserve development.

**Payee (claimant, vendor)**
- As a payee, I want to choose electronic payment and get notified when it's sent.

**Edge cases**
- Payment exceeds outstanding reserve → combined "increase reserve and pay" within authority, or routed for approval.
- Check voided after 90 days (stale-dated) → reversal, reserve reinstated, reissue workflow.
- Overpayment discovered → recovery from payee.
- Claimant represented by attorney → payee must include attorney per jurisdiction rules.

## 5. Domain Model & Invariants

| Entity | Notes |
|---|---|
| **Reserve** (per claim line × cost category) | current amount (projection), history of **Reserve Changes** (amount delta, new total, reason code, method `RULE_INITIAL`/`CASE`/`SYSTEM_EROSION`, actor, authority decision, idempotency key) |
| **Settlement** | claim line, agreed amount, components (indemnity breakdown), liability factor applied, deductible, limits check, release document (F06), status (`PROPOSED`, `AGREED`, `PAID`, `RESCINDED`) |
| **Payment Request** (claims-side) | claim line(s), cost category, payees + allocation, payment type (`PARTIAL`, `FINAL`, `SUPPLEMENTAL`, `EXPENSE`), method, invoice ref (V01), deductible application, tax-reporting data refs, linked Payment Order (F04) |
| **Recovery** | type (`SUBROGATION`, `SALVAGE`, `DEDUCTIBLE_RECOVERY`, `OVERPAYMENT`, `CONTRIBUTION`), claim line(s), target party, expected amount (recovery reserve), state, demands, receipts |
| **Financial Summary** (projection) | per line/claim/policy term: reserved, paid, outstanding, recovered, recovery reserve, net incurred, by cost category |

**Recovery state machine**: `IDENTIFIED → OPEN → (DEMAND_SENT → NEGOTIATING | ARBITRATION | LITIGATION) → (RECOVERED_FULL | RECOVERED_PARTIAL | ABANDONED)`; `IDENTIFIED → DISMISSED (reason)`.

Payment Order lifecycle is owned by F04; claims tracks it by reference and reacts to its events.

**Financial movements & posting (via F04 posting rules)**

| Movement | Trigger | Effect |
|---|---|---|
| Initial reserve | Line opened | Reserve established (rule-suggested, adjuster-confirmed) |
| Reserve change | Adjuster/system | Delta posted; reason required |
| Payment release | Payment Order `RELEASED` | Paid posted; reserve eroded by paid amount (floor 0, configurable "erode vs. keep") |
| Payment void/return | Payment Order `VOIDED`/`RETURNED` | Reversal; reserve reinstated per rule |
| Recovery reserve | Recovery opened | Expected recovery tracked (separately from case reserve) |
| Recovery receipt | Receipt allocated | Recovery posted; recovery reserve reduced |

**Invariants**
- INV-C05-1: Outstanding reserve per line × category ≥ 0 at all times.
- INV-C05-2: Every reserve change and payment request carries a permanent idempotency key (CRC-IDEM-003).
- INV-C05-3: A payment can be approved only if: line `OPEN`; coverage position `COVERED`/`PARTIALLY_COVERED` (or `RESERVATION_OF_RIGHTS` where configured to allow payment under ROR); amount ≤ available limit (C04 serialization); amount ≤ outstanding reserve (or a paired reserve increase within authority); approver authority ≥ amount (cumulative rules); SoD; no blocking fraud hold.
- INV-C05-4: Deductible is applied once per occurrence per the snapshot's deductible structure across all payments on the line (tracked deductible balance).
- INV-C05-5: Financial Summary equals ledger balances for the same dimensions (continuous reconciliation, F04-P0-09).
- INV-C05-6: Payee data (bank details) used is the approved version (F04 INV-F04-8); claimant-to-vendor/attorney payee rules per jurisdiction enforced (F05).
- INV-C05-7: AI cannot create reserve changes, payment requests, or approvals (CRC-AI-002); it may only recommend.

## 6. Requirements

### P0 — Must-have

**C05-P0-01 Initial reserves.** Rule-based initial reserve per segment/coverage/cost category (F05, average or formula), established at line open; adjuster confirms or adjusts with reason.
*Acceptance:* every opened line has a reserve per required cost category within the configured time; lines without reserves appear as exceptions.

**C05-P0-02 Reserve changes.** Commands to increase/decrease with reason code and authority; above-authority changes route to approval (C03); history with actor and authority decision; large-loss thresholds trigger flags and notifications.
*Acceptance:* a decrease that would make outstanding negative is rejected with `RESERVE_WOULD_BE_NEGATIVE`.

**C05-P0-03 Payment requests.** Multi-payee allocation (exact sum), payment types, methods, deductible application, limit check (C04), reserve check, duplicate detection (F04-P0-06 plus claims-specific: same invoice, same estimate, same service dates), tax data capture, create F04 Payment Order.
*Acceptance:*
- Given an adjuster with authority 5,000 and a 7,500 request, the payment order is `PENDING_APPROVAL` and appears in an eligible approver's queue with full context.
- Given a retried request with the same idempotency key, exactly one payment order exists.

**C05-P0-04 Combined reserve-and-pay.** Single command that increases reserve and requests payment atomically within authority.

**C05-P0-05 Settlements.** Record settlement offers/agreements with liability factor (C04), limits, deductible, release document requirement; final payment closes outstanding to 0 (or prompts reserve reduction) and suggests line closure (C02).

**C05-P0-06 Payment status & lifecycle reactions.** React to F04 payment-order events: on release erode reserve; on void/return reverse and reinstate reserve per rule, create follow-up work item; expose payment status to portals.

**C05-P0-07 Expense payments.** Vendor invoices approved in V01 → expense payment requests on the correct cost category; litigation expense (defense counsel) tracking.

**C05-P0-08 Recoveries.** Identify (rules from liability assessment, AI suggestion as recommendation), open with recovery reserve, demands (template letters via X04), receipts allocation (F04 receipts), close; salvage with vendor assignment (V01), title status, proceeds; deductible recovery on ground-up payments.
*Acceptance:* every claim line with `TOTAL_LOSS` flag has either a salvage recovery or a dismissal with reason.

**C05-P0-09 Financial summary & as-of.** Per line/claim/policy: reserved, paid, outstanding, recovered, net incurred, by category; as-of (accounting date) queries backed by ledger.

**C05-P0-10 Fraud & sanctions holds.** Payment approval blocked if R03 places a hold; sanctions screening happens in F04 before release; holds visible with reason to authorized roles only.

### P1 — Fast follow
- **C05-P1-01** Recurring/scheduled payments (rental, ALE, periodic indemnity) with end conditions.
- **C05-P1-02** Reserve adequacy reviews: diaries and AI-assisted reserve recommendation (L2) based on similar closed claims, with explanation.
- **C05-P1-03** Inter-company subrogation arbitration integration (S02).
- **C05-P1-04** Payment preferences portal for claimants (choose method, verify bank via instant verification).
- **C05-P1-05** Escheatment coordination for uncashed claim checks (with F04-P1-02).

### P2 — Future
- **C05-P2-01** Structured settlements.
- **C05-P2-02** Reinsurance recoverables at claim level.
- **C05-P2-03** Virtual-card and instant payouts to vendors.

## 7. Interfaces & Events
- APIs: `/v1/claim-lines/{id}/reserves` (changes), `/v1/claim-lines/{id}/payment-requests`, `/v1/claim-lines/{id}/settlements`, `/v1/claims/{id}/recoveries`, `/v1/recoveries/{id}/demands|receipts|close`, `GET /v1/claims/{id}/financial-summary?asOf=`.
- **Events published**: `claims.reserve.changed.v1`, `claims.payment.requested.v1`, `claims.payment.approved.v1`, `claims.settlement.agreed.v1`, `claims.recovery.identified.v1`, `claims.recovery.opened.v1`, `claims.recovery.received.v1`, `claims.recovery.closed.v1`, `claims.large-loss.threshold-crossed.v1`.
- **Events consumed**: `claims.claim-line.opened.v1`, `claims.coverage.determined.v1`, `claims.liability.assessed.v1`, `ledger.payment-order.*`, `ledger.receipt.recorded.v1`, `vendors.invoice.approved.v1`, `fraud.hold.placed.v1` / `…released.v1`.

## 8. AI Integration Points
- **AI04** may include an initial reserve *recommendation* in triage output (L2); rules set the initial reserve, adjuster confirms.
- P1 reserve adequacy recommendations (L2) with similar-claim evidence.
- **AI02** extracts invoice/estimate line items for validation (V01) — proposals only.
- **AI06** drafts subrogation demand letters (L3).
- Hard denylist: no AI reserve/payment/approval tools (CRC-AI-002).

## 9. Audit, Security & Compliance
- Full audit per movement with authority result; approval chains visible in timeline.
- Prompt-payment regulation: time from settlement agreement to payment tracked against C02 deadlines.
- Payee verification and bank-change controls (F04, F01 SoD); tax reporting data for payees where required.

## 10. Success Metrics

| Metric | Type | Target (hypothesis) |
|---|---|---|
| Claims ⇄ ledger variance | Leading | 0 |
| Payments outside authority without approval | Leading | 0 |
| Platform-caused duplicate payments | Lagging | 0 |
| Duplicate detection on seeded cases | Leading | ≥ 95% |
| Initial reserve accuracy (closed claims, by segment) | Lagging | +20% vs. baseline at 12 months |
| Subrogation identification on eligible claims | Lagging | ≥ 90% |
| Settlement agreed → payment released p90 | Leading | < 2 business days |

## 11. Open Questions
- [Finance/Claims] [Blocking] Reserve erosion policy on payment ("erode to floor 0" vs. "adjuster must re-reserve").
- [Claims] [Blocking] Cost category taxonomy per LOB (indemnity, ALAE/DCC, A&O, medical, rental, ALE…).
- [Legal] [Non-blocking] Payee rules for represented claimants and lienholders per jurisdiction.
- [Claims] [Non-blocking] Payment under ROR permitted per product?

## 12. Dependencies & Phasing
- **First**: reserves (initial + changes) posting to F04, payment requests → F04 payment orders with authority/approval, financial summary — required for any real claim.
- **Then**: settlements, recoveries (subrogation/salvage), expense payments from V01.
- **Parallel**: recurring payments, reserve adequacy AI, arbitration integration, payee preferences UI.
