# PRD-C03 — Assignment, Triage, Investigation & Work Management

| Field | Value |
|---|---|
| ID | PRD-C03 |
| Domain | Claims |
| Tier | **DEEP** (work-item, SLA, and authority-routing kernel); routing-rule sophistication is parallelizable |
| Status | Draft v0.1 |
| Owner | Claims Workflow (TBD) |
| Depends on | C02 (claim lifecycle, deadlines), F01 (skills, licenses, authority), F05 (triage/assignment rules), F06 (document requests), V01 (vendor assignments), R03 (SIU), AI04 |
| Consumed by | C04, C05 (approval routing), F07 (approval gates as work items), X04, A01 |
| Key contract rules | CRC-SM-001…009; CRC-SEC-003; CRC-AUD-004; CRC-CFG-001, 002; CRC-AI-012 |

## 1. Problem Statement

The same claim handled by the right adjuster on day one costs far less than one that bounces between queues, sits unworked, or lands with someone unlicensed in the loss jurisdiction. Most carriers route on a few hard-coded rules and track follow-ups in personal calendars, so supervisors can't see workload, SLAs are missed, and investigation steps are inconsistent. The platform needs a configurable triage and assignment engine plus a universal work-item model (tasks, diaries, approvals, document requests) with SLAs, so every claim has an owner, a next step, and a due date.

## 2. Goals

1. **Right first assignment**: ≥ 90% of claims never require reassignment for skill/licensing/authority reasons (hypothesis).
2. **No orphaned work**: 0 open claims without an owner and at least one open work item or scheduled diary.
3. **SLA adherence**: ≥ 95% of work items completed within SLA; 100% of SLA breaches visible to supervisors before they occur (due-soon alerts).
4. **Fast-track efficiency**: ≥ 30% of eligible low-complexity personal-lines claims handled in fast-track/straight-through segments with leakage no worse than standard handling.
5. **Time to first contact** p90 < 1 business day.

## 3. Non-Goals

- **Workforce scheduling and HR capacity planning** (shift scheduling) — capacity is consumed as input.
- **General-purpose BPM.** Work items and investigation plans are claims-specific configuration, not a BPMN modeler.
- **Fully automated claim decisions.** Straight-through processing is bounded by deterministic rules and system authority limits; never AI-driven payment or denial.

## 4. Personas & User Stories

**Claims supervisor / team lead**
- As a supervisor, I want a live view of my team's inventory, workload, aging, SLA status, and authority queue so that I can rebalance before deadlines slip.
- As a supervisor, I want to reassign claims in bulk with a reason so that I can handle absences and surges.

**Adjuster**
- As an adjuster, I want a prioritized work queue across all my claims ("what should I do next") with due dates and reasons.
- As an adjuster, I want an investigation checklist generated for the claim type (e.g., recorded statement, police report, inspection) that I can adapt, so that nothing is missed.
- As an adjuster, I want to diary a follow-up and have it reappear on the due date.

**Triage specialist / intake**
- As a triage specialist, I want the system's recommended segment and complexity with reasons, and to override with a reason when I disagree.

**Approver**
- As an approver, I want reserve/payment approvals above others' authority to arrive in my queue with all context so that I can approve in one step.

**Edge cases**
- No eligible adjuster (licensing/capacity) → claim goes to an overflow queue with supervisor alert; never auto-assigned to an ineligible person.
- Adjuster leaves the company → all open work is reassigned by rule within 1 business day.
- Claim flagged `LITIGATION` mid-handling → reassignment to litigation unit per rule, with handover note.

## 5. Domain Model & Invariants

| Entity | Notes |
|---|---|
| **Triage Result** | claim, segment (config: `STRAIGHT_THROUGH`, `FAST_TRACK`, `STANDARD`, `COMPLEX`, `LARGE_LOSS`, `SIU`), complexity score, severity, priority, reasons, rule-set version, AI recommendation ref, final decided-by |
| **Assignment** | claim or claim line → handler (principal) and/or queue; role (`PRIMARY_ADJUSTER`, `LINE_ADJUSTER`, `SUPERVISOR`, `LITIGATION`, `SIU`, `FIELD_APPRAISER`); effective period; reason |
| **Queue** | skill requirements, jurisdictions, LOBs, segment, members, capacity model |
| **Work Item** | type (`TASK`, `DIARY`, `APPROVAL`, `DOCUMENT_REQUEST_FOLLOWUP`, `DEADLINE`, `AI_PROPOSAL_REVIEW`, `MANUAL_MATCH`, `RECONCILIATION_BREAK`, …), subject refs, assignee/queue, priority, due-at, SLA policy, state, outcome, origin (rule/user/AI/deadline) |
| **Investigation Plan** | template per (LOB, cause of loss, segment) → activities (recorded statement, scene/vehicle inspection, police/fire report request, medical records request, expert/IA assignment via V01, coverage investigation, SIU referral), each instantiating work items |
| **Note** | claim note with category, visibility (internal/SIU/privileged), author, linked entities (immutable; corrections append) |

**Work Item states**: `OPEN → IN_PROGRESS → COMPLETED` ; `OPEN/IN_PROGRESS → CANCELLED (reason)` ; `OPEN → ESCALATED → (IN_PROGRESS | COMPLETED)`; snooze sets a new due-at with reason (audited) but not a state change.

**Invariants**
- INV-C03-1: Every `OPEN` claim has exactly one primary handler (principal or queue) at all times.
- INV-C03-2: Assignment eligibility (license in loss jurisdiction where required, LOB skill, authority sufficient for segment's typical financial level, language if required, conflict-of-interest exclusions) is checked at assignment time; ineligible assignments are rejected unless supervisor overrides with reason (audited override).
- INV-C03-3: Approval work items can be completed only by principals whose authority covers the request (F01), and not by the requester where SoD applies.
- INV-C03-4: Triage decisions record rule version + AI recommendation (if any) + final decision; human differences are overrides (CRC-AUD-004).
- INV-C03-5: Straight-through processing (STP) may only execute actions whose rule outcome is deterministic and whose amount is within the configured system-principal authority; every STP action is audited with actor type `SYSTEM`.

## 6. Requirements

### P0 — Must-have

**C03-P0-01 Triage at registration and on change.** Evaluate triage rules (F05) with facts from the claim, snapshot, parties, AI04 recommendation, and fraud score (R03); re-triage on material events (injury added, litigation, reserve crossing thresholds).
*Acceptance:* Given AI04 recommends `FAST_TRACK` but a rule requires `STANDARD` for injury claims, the triage result is `STANDARD` with both recorded and the disagreement logged (CRC-AI-012).

**C03-P0-02 Assignment engine.** Rule-based routing to queue and/or handler using skills, licenses (by jurisdiction), authority, capacity/workload, language, geography, round-robin/least-loaded strategies; manual assignment and reassignment with reason; bulk reassignment.

**C03-P0-03 Work items & queues.** Universal work-item model with SLA policies (business calendars per jurisdiction/team), priority, due-soon and overdue escalations, personal and team queues, "next best work" ordering.

**C03-P0-04 Diaries.** Scheduled follow-ups per claim; mandatory diary rules (e.g., every open claim has a diary within N days; large-loss review every 30 days).

**C03-P0-05 Approval routing.** Reserve/payment/settlement/coverage approvals above requester authority route to eligible approvers (F01), with escalation if not actioned within SLA.

**C03-P0-06 Investigation plans.** Configurable templates producing activities as work items; adapt per claim with audit; activities link to document requests (F06) and vendor assignments (V01).

**C03-P0-07 SIU referral.** Referral from rules, scores, or adjuster; SIU acceptance/decline with reason; SIU-only notes and visibility (R03 owns case management).

**C03-P0-08 Notes.** Categorized, immutable notes with visibility classes; privileged notes restricted to litigation/legal roles.

**C03-P0-09 Supervisor console.** Inventory, aging, workload, SLA heatmap, authority queue, unassigned/overflow, reassignment actions.

**C03-P0-10 AI proposal review items.** F07 approval gates appear as work items with embedded approve/edit/reject actions.

**C03-P0-11 Straight-through processing (bounded).** Rule-defined STP paths (e.g., glass repair with in-network vendor under threshold) that perform registration → coverage confirmation (C04 rules) → vendor assignment → payment request within system authority; any rule exception drops to human handling.

### P1 — Fast follow
- **C03-P1-01** Capacity-aware forecasting and CAT surge routing (temporary queues, external IA capacity via V01).
- **C03-P1-02** Skill matrix management UI and adjuster license sync (S02).
- **C03-P1-03** Handling-quality reviews (QA sampling with scorecards) linked to timelines.
- **C03-P1-04** Customer contact tracking (time to first contact, contact attempts) with reminders.

### P2 — Future
- **C03-P2-01** Optimization-based assignment (maximize match quality under capacity).
- **C03-P2-02** Field appraiser routing and scheduling with maps (X03 field app).

## 7. Interfaces & Events
- APIs: `/v1/claims/{id}/triage`, `/v1/claims/{id}/assignments`, `/v1/work-items`, `/v1/work-items/{id}/start|complete|cancel|escalate|reassign`, `/v1/queues`, `/v1/claims/{id}/notes`, `/v1/claims/{id}/investigation-plan`.
- **Events published**: `claims.claim.triaged.v1`, `claims.claim.assigned.v1`, `claims.claim.reassigned.v1`, `claims.work-item.created.v1`, `…completed.v1`, `…overdue.v1`, `…escalated.v1`, `claims.siu-referral.requested.v1`.
- **Events consumed**: `claims.claim.registered.v1`, `claims.claim.flag-set.v1`, `claims.deadline.due-soon.v1`, `aiharness.proposal.created.v1`, `ledger.payment-order.requested.v1` (approval needed), `identity.principal.deactivated.v1`, `vendors.assignment.*`, `documents.request.overdue.v1`.

## 8. AI Integration Points
- **AI04** triage & prioritization recommendations (L2) as facts into triage rules.
- **AI05** fraud summaries on SIU referral items (L2, internal).
- **AI06** drafts contact attempts and status letters from work items (L3).
- P1: "next best action" suggestions on a claim (L2), never auto-executed.

## 9. Audit, Security & Compliance
- Assignments, reassignments, triage overrides, eligibility overrides, and approval outcomes audited.
- Licensing compliance: adjuster license checks per jurisdiction recorded at assignment.
- Notes visibility enforced by PDP; SIU/privileged notes never visible to claimants or vendors.

## 10. Success Metrics

| Metric | Type | Target (hypothesis) |
|---|---|---|
| Reassignments due to eligibility | Leading | < 10% of claims |
| Open claims without owner/next action | Leading | 0 |
| Work items within SLA | Lagging | ≥ 95% |
| Time to first contact p90 | Leading | < 1 business day |
| Fast-track share of eligible claims | Lagging | ≥ 30% |
| Fast-track vs. standard leakage (QA sample) | Lagging | No worse than standard |

## 11. Open Questions
- [Claims] [Blocking] Segment taxonomy and launch triage rules per LOB.
- [Claims] [Non-blocking] Adjuster licensing data source per market (manual vs. licensing registry via S02).
- [Product] [Non-blocking] Which STP paths at launch (glass, towing, small property)?

## 12. Dependencies & Phasing
- **First**: work-item model, queues, SLA calendars, approval routing (needed by C05 and F07 gates), assignment with eligibility.
- **Then**: triage rules integration, investigation plans, supervisor console.
- **Parallel**: advanced routing strategies, QA reviews, AI04 shadow evaluation.
