# PRD-C02 — Claim Lifecycle & State Machine

| Field | Value |
|---|---|
| ID | PRD-C02 |
| Domain | Claims (primary domain) |
| Tier | **DEEP** — the backbone of the platform |
| Status | Draft v0.1 |
| Owner | Claims Core (TBD) |
| Depends on | P02 (CoverageSnapshot), P01 (claim settings), F01, F02, F03, F05, F06 |
| Consumed by | C01, C03, C04, C05, V01, X01–X04, R02, R03, A01, AI04–AI06 |
| Key contract rules | CRC-SM-001…010; CRC-AUD-001…004; CRC-IDEM-*; CRC-EVT-*; CRC-DOM-004, 005, 012; CRC-TIME-006 |

## 1. Problem Statement

A claim is a long-running, multi-party, regulated process: it is registered, investigated, evaluated, paid, recovered, closed, sometimes reopened or litigated, across multiple claimants and coverages. When lifecycle is implicit — status fields that any screen can set, closure allowed with money outstanding, reopen without reason — the result is leakage, regulatory violations (missed acknowledgement and decision deadlines), and claims nobody can explain. The platform needs an explicit, enforced state machine for the claim and its claim lines, with invariants tying lifecycle to coverage and money, and a complete audited history that lets a claim move from FNOL to final settlement with full traceability.

## 2. Goals

1. **Enforced lifecycle**: 0 claims or claim lines in states that violate invariants (e.g., closed with outstanding reserve), verified by continuous invariant checks.
2. **End-to-end auditability**: 100% of transitions have audit records with actor, reason, and causation; claim timeline reconstructs the full lifecycle.
3. **Regulatory timeliness**: ≥ 99.5% of acknowledgement, decision, and payment deadlines met (configurable per jurisdiction), with breaches visible before they occur.
4. **Configuration-driven LOB support**: new LOBs use the same claim model with no subclassing (CRC-DOM-012).
5. **Throughput**: registration p95 < 500 ms; supports CAT surges of 50× daily average registration volume.

## 3. Non-Goals

- **Intake UX** (C01), **work routing** (C03), **coverage logic** (C04), **money** (C05) — this PRD defines the lifecycle and the cross-cutting invariants they must respect.
- **Litigation case management** (court calendars, counsel billing) beyond flags, roles, and holds — P2/external.
- **Workers' compensation–specific benefit regimes** at launch (pattern-compatible, P2).

## 4. Personas & User Stories

**Adjuster**
- As an adjuster, I want to see a claim's current state, allowed next actions, and what is blocking closure (e.g., "Line 2: outstanding expense reserve 500") so that I can move it forward without guesswork.
- As an adjuster, I want to add a claimant and a new claim line when a third party comes forward so that each claimant's coverage and money are tracked separately.
- As an adjuster, I want to correct the loss date with a reason and have coverage re-checked so that the right policy version applies.

**Claims supervisor**
- As a supervisor, I want reopen requests to require a reason and (above thresholds) my approval so that reopen rates reflect real need.
- As a supervisor, I want to see claims approaching regulatory deadlines so that we never miss them.

**Litigation specialist**
- As a litigation specialist, I want a litigation flag that reassigns the claim, places documents on legal hold, and records counsel parties.

**Claimant / insured**
- As a claimant, I want my claim's status in plain language so that I know what happens next (mapped from internal states, X01).

**Compliance**
- As a compliance officer, I want every claim to record jurisdiction-specific deadlines and their outcomes so that market-conduct exams are straightforward.

**Edge cases**
- Claim registered on the wrong policy → re-point policy with reason; coverage snapshot rebinds; financial postings (if any) require reversal before re-point.
- Claim registered in error → void only if no financial postings ever existed.
- All claim lines closed but a subrogation recovery is still being pursued.
- Retroactive policy change after registration (`policy.version.retroactively-changed.v1`).

## 5. Domain Model & Invariants

### 5.1 Aggregates

| Aggregate | Key content |
|---|---|
| **Claim** | claimNumber, tenant, policyRef (+ `POLICY_UNVERIFIED` until matched), coverageSnapshotId, loss (date/time + precision + zone, location, cause of loss, description), jurisdiction, LOB/product version, state, handling stage, flags, CAT event code, reported-at, registered-at, deadlines, reopen count |
| **Claim Line** | claimLineId, claimId, claimant party, coverage code (from snapshot), risk object, line type (config), state, coverage position ref (C04), liability ref (C04), assigned handler (C03), reserve summary (projection from C05) |
| **Party (claim role)** | party ref, roles per claim / claim line (`INSURED`, `CLAIMANT`, `DRIVER`, `WITNESS`, `ATTORNEY`, `PUBLIC_ADJUSTER`, `LIENHOLDER`, `MORTGAGEE`, `VENDOR`, `OTHER_CARRIER`), representation (claimant represented by attorney X), contact preferences |
| **Recovery** | (C05 detail) linked to claim / claim line, own lifecycle |

Claim Line is its own aggregate (CRC-DOM-004): high-contention multi-line claims don't serialize on the claim header.

### 5.2 Claim state machine

```
                 register                     assign/accept
 (Loss Report) ───────────▶ REGISTERED ─────────────────────▶ OPEN
                               │                               │  ▲
                               │ void                          │  │ reopen (reason, authority)
                               ▼                               │  │
                            VOIDED                close (all lines closed/voided)
                                                               ▼  │
                                                             CLOSED
```

| From | Command | Guards (all must pass) | To | Events |
|---|---|---|---|---|
| — | `RegisterClaim` | loss report submitted; idempotency key unused; loss date ≤ report date | `REGISTERED` | `claims.claim.registered.v1` |
| `REGISTERED` | `OpenClaim` (on first assignment) | assigned handler or queue exists | `OPEN` | `claims.claim.opened.v1` |
| `REGISTERED`/`OPEN` | `VoidClaim` | no ledger postings ever on any line; supervisor authority; reason ∈ void reasons | `VOIDED` | `claims.claim.voided.v1` |
| `OPEN` | `CloseClaim` | every claim line `CLOSED` or `VOIDED`; no payment orders non-terminal; reason | `CLOSED` | `claims.claim.closed.v1` |
| `CLOSED` | `ReopenClaim` | within reopen window (config); reason; authority (supervisor above thresholds) | `OPEN` | `claims.claim.reopened.v1` |

Handling **stage** (`TRIAGE`, `INVESTIGATION`, `EVALUATION`, `NEGOTIATION`, `SETTLEMENT`, `RECOVERY_ONLY`) is an informational sub-status set by C03 actions; it never gates transitions on its own.

### 5.3 Claim Line state machine

| From | Command | Guards | To | Events |
|---|---|---|---|---|
| — | `OpenClaimLine` | claim `REGISTERED`/`OPEN`; coverage code exists in snapshot (or `PENDING_POLICY_MATCH`); claimant party present; initial reserve request accompanies (C05) | `OPEN` | `claims.claim-line.opened.v1` |
| `OPEN` | `CloseClaimLine` | outstanding reserve = 0 for all cost categories; no non-terminal payment orders; coverage position ≠ `PENDING`; closure outcome (`PAID`, `DENIED`, `WITHDRAWN`, `NO_PAYMENT_REQUIRED`, `DUPLICATE`) + reason | `CLOSED` | `claims.claim-line.closed.v1` |
| `CLOSED` | `ReopenClaimLine` | claim reopened or reopen-in-place permitted; reason; reserve re-establishment request | `OPEN` | `claims.claim-line.reopened.v1` |
| `OPEN` | `VoidClaimLine` | no ledger postings ever; reason | `VOIDED` | `claims.claim-line.voided.v1` |

Orthogonal dimensions on the Claim Line (CRC-SM-010): **coverage position** (C04), **liability** (C04), **financial position** (C05). Cross-dimension rules are expressed as guards above.

### 5.4 Flags (independent, audited set/clear commands)

`LITIGATION`, `ATTORNEY_REPRESENTED`, `SIU_REFERRED`, `LARGE_LOSS`, `CATASTROPHE`, `COMPLAINT` (regulatory/DOI complaint), `INJURY`, `FATALITY`, `TOTAL_LOSS`, `SUBROGATION_POTENTIAL`, `COVERAGE_IN_QUESTION`, `FAST_TRACK`, `VIP/SENSITIVE`. Flag effects are configured (F05): e.g., `LITIGATION` → C03 reassignment rule, F06 legal hold, notification to legal.

### 5.5 Deadlines (regulatory and service)

Deadline definitions are configuration per jurisdiction × LOB (F05): e.g., *acknowledge within N days of notice*, *accept/deny within N days of proof of loss*, *pay within N days of agreement*, *status letter every N days while open*. Each claim materializes deadline instances with due dates computed from business events, tracked to `MET` / `BREACHED` / `NOT_APPLICABLE`, and drives C03 work items and X04 notices.

### 5.6 Invariants
- INV-C02-1: A claim's state changes only through the commands above (CRC-SM-007).
- INV-C02-2: `CLOSED` claim ⇒ all lines `CLOSED`/`VOIDED` and no non-terminal payment orders.
- INV-C02-3: `CLOSED` line ⇒ outstanding reserves = 0 (all categories) and coverage position resolved.
- INV-C02-4: `VOIDED` ⇒ no ledger entries ever referenced the claim/line.
- INV-C02-5: Claim binds exactly one coverage snapshot at a time; any rebinding (loss date change, policy re-point, retroactive policy change) is audited and triggers coverage re-evaluation (C04) for all open lines.
- INV-C02-6: Recoveries may remain open on a `CLOSED` claim; recovery receipts do not reopen the claim unless configured.
- INV-C02-7: Claim number is unique per tenant, immutable, and never reused (even if voided).
- INV-C02-8: Every party with access-relevant roles (claimant, attorney, vendor) produces relationship grants (F01) scoped to the relevant lines.

## 6. Requirements

### P0 — Must-have

**C02-P0-01 Register claim.** Idempotent `RegisterClaim` from C01 (or API) creating Claim + initial lines (from FNOL damaged objects/claimants mapped to snapshot coverages) + parties; claim number generation (configurable format); coverage snapshot binding (P02) as of loss date/time with precision handling (CRC-TIME-006).
*Acceptance:*
- Given a matched loss report, when registered, then a claim with state `REGISTERED`, bound snapshot ID, and initial lines exists, and `claims.claim.registered.v1` is published once even under retries.
- Given loss precision `DATE_ONLY` on a date where the policy incepted mid-day, then coverage binding flags `LOSS_TIME_AMBIGUOUS` and C04 receives both candidate snapshots.

**C02-P0-02 Explicit state machines** for Claim and Claim Line per §5, implemented per CRC-SM-001…006, with generated diagrams and full transition tests.
*Acceptance:* every illegal (state, command) pair returns `409` with problem type `illegal-state-transition` and is covered by a test.

**C02-P0-03 Closure with blockers.** Close commands evaluate all guards and return *all* blocking reasons (CRC-SM-003); "allowed actions" query API for UIs.

**C02-P0-04 Reopen with reason & authority.** Reopen reasons (config), authority thresholds, reopen window; reopened lines require reserve re-establishment (C05).

**C02-P0-05 Void with financial guard.** Void only when INV-C02-4 holds; supervisor authority; claim number retired.

**C02-P0-06 Multi-party model.** Parties with roles per claim and per line; add/remove roles with reason; representation (attorney/public adjuster) with contact routing (communications go to representative when represented — X04 rule).

**C02-P0-07 Claim lines.** Add lines for new claimants/coverages at any time while open; line per claimant × coverage × risk object; coverage code must exist on the bound snapshot or line is `COVERAGE_NOT_ON_POLICY` (C04 handles denial).

**C02-P0-08 Loss detail corrections.** Loss date/time, location, cause of loss editable with reason; date/time change rebinds snapshot (INV-C02-5) and triggers re-evaluation.

**C02-P0-09 Policy re-point & unverified policy.** Match an unverified claim to a policy; re-point a mismatched claim (guard: no postings, or postings reversed first).

**C02-P0-10 Flags** with configured effects; litigation flag → legal hold (F06), reassignment (C03), notifications.

**C02-P0-11 Deadlines.** Jurisdictional deadline engine per §5.5 with configurable definitions, computed due dates, work-item creation ahead of due (C03), and breach recording.
*Acceptance:* Given a jurisdiction configured with "acknowledge within 15 days," when a claim is registered and no acknowledgement communication is sent by day 12, then a high-priority work item exists; at day 15 without acknowledgement the deadline is `BREACHED` and reported (R02).

**C02-P0-12 Retroactive policy change handling.** On `policy.version.retroactively-changed.v1` affecting the bound snapshot, rebind and create a coverage review work item (default) for open lines.

**C02-P0-13 Customer-facing status mapping.** Configurable mapping from (state, stage, flags, deadlines) to plain-language statuses for X01/X03, never exposing internal flags such as SIU.

**C02-P0-14 Claim timeline.** Unified timeline view (F02 + C03 notes + communications + documents + financial events).

**C02-P0-15 CAT events.** Create CAT event codes (name, peril, area, period); tag claims manually or by rule; CAT-mode configuration (relaxed deadlines where regulators allow, surge routing).

### P1 — Fast follow
- **C02-P1-01** Claim merge/split (duplicate claims merged with line transfer, financial guard).
- **C02-P1-02** Related-claims linking (same loss across policies, multi-carrier subrogation).
- **C02-P1-03** Bulk operations for CAT (mass status letters, bulk reassignment) with per-claim audit.
- **C02-P1-04** Complaint register integration (DOI complaints linked to claims, deadlines).

### P2 — Future
- **C02-P2-01** Workers' compensation / disability patterns (benefit periods, recurring indemnity) using the same state machine with extended line types.
- **C02-P2-02** Litigation management module (counsel assignment, budgets, court dates).
- **C02-P2-03** Reinsurance notifications for large losses (ceded recoveries).

## 7. Interfaces & Events

**Commands (REST, CRC-API-004)**: `POST /v1/claims` (register), `/v1/claims/{id}/open|close|reopen|void`, `/v1/claims/{id}/lines`, `/v1/claim-lines/{id}/close|reopen|void`, `/v1/claims/{id}/parties`, `/v1/claims/{id}/flags`, `/v1/claims/{id}/loss-details/corrections`, `/v1/claims/{id}/policy-match`, `GET /v1/claims/{id}/allowed-actions`, `GET /v1/claims/{id}/timeline`.

**Events published**: `claims.claim.registered.v1`, `…opened.v1`, `…closed.v1`, `…reopened.v1`, `…voided.v1`, `claims.claim.flag-set.v1`, `…flag-cleared.v1`, `claims.claim.litigation-flagged.v1`, `claims.claim.coverage-snapshot-rebound.v1`, `claims.claim.loss-details-corrected.v1`, `claims.claim-line.opened.v1`, `…closed.v1`, `…reopened.v1`, `…voided.v1`, `claims.party.added.v1`, `…role-changed.v1`, `claims.deadline.due-soon.v1`, `…met.v1`, `…breached.v1`.

**Events consumed**: `fnol.loss-report.submitted.v1`, `policy.version.retroactively-changed.v1`, `claims.coverage.determined.v1` (C04), `claims.reserve.changed.v1` (C05 projection for guards), `ledger.payment-order.*` (non-terminal tracking), `fraud.siu-referral.accepted.v1`.

## 8. AI Integration Points
- **AI04** recommends segment/priority at registration (consumed by C03).
- **AI06** drafts acknowledgement and status letters driven by deadlines (L3; acknowledgement may be L4 once evals pass).
- **AI05** summarization surfaces on flagged claims (internal).
- AI never closes, voids, reopens, or changes coverage position (CRC-AI-002).

## 9. Audit, Security & Compliance
- Every command audited with reason codes; deadlines and their outcomes persisted for regulatory reporting (R02).
- SIU flag and fraud data hidden from claimant-facing views and from roles without SIU permission.
- Legal hold on litigation; PHI-bearing lines (injury) restrict access to PHI-permitted roles.

## 10. Success Metrics

| Metric | Type | Target (hypothesis) |
|---|---|---|
| Invariant violations detected by continuous checks | Leading | 0 |
| Transitions with complete audit | Leading | 100% |
| Regulatory deadlines met | Lagging | ≥ 99.5% |
| Registration p95 | Leading | < 500 ms |
| Reopen rate (30/90 days after close) | Lagging | Baseline, then −20% |
| Cycle time FNOL → first payment (per segment) | Lagging | Baseline, then −25% |

## 11. Open Questions
- [Claims] [Blocking] Launch jurisdictions' deadline definitions (content for §5.5).
- [Claims] [Blocking] Default line creation at FNOL: auto-create lines from FNOL data or adjuster creates during triage?
- [Engineering] [Non-blocking] Claim Line as separate aggregate confirmed? (Recommended for contention; ADR.)
- [Legal] [Non-blocking] Reopen window defaults and whether reopening requires claimant notice.

## 12. Dependencies & Phasing
- **First**: Claim & Claim Line aggregates, state machines, register/close/reopen/void, parties, coverage snapshot binding (needs P02 snapshot API, which can be stubbed initially).
- **Then**: deadlines engine, flags with effects, retroactive-change handling, status mapping.
- **Parallel**: C01 intake, C03 routing rules, X-portals views, AI04/AI06 in shadow.
