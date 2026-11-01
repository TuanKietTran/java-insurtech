# PRD-<ID> — <Title>

| Field | Value |
|---|---|
| ID | PRD-<ID> |
| Domain | <Domain> |
| Tier | **DEEP** or **PARALLEL** |
| Status | Draft v0.1 |
| Owner | <team> (TBD) |
| Depends on | <PRD IDs and the specific interfaces needed> |
| Consumed by | <PRD IDs> |
| Key contract rules | <CRC-* IDs> |

## 1. Problem Statement
2–3 sentences: the user problem, who has it, the cost of not solving it.

## 2. Goals
3–5 measurable outcomes (user and business).

## 3. Non-Goals
3–5 explicit exclusions, each with a one-line rationale.

## 4. Personas & User Stories
Grouped by persona, most important first. "As a <specific user>, I want <capability> so that <benefit>." Include edge and error cases.

## 5. Domain Model & Invariants
Aggregates, states and transitions, invariants that must always hold. (DEEP PRDs: mandatory and detailed. PARALLEL PRDs: scope and owned entities.)

## 6. Requirements
### P0 — Must-have
`<ID>-P0-01` **Title.** Behavior. *Acceptance:* Given/When/Then or checklist.
### P1 — Fast follow
### P2 — Future (design must not preclude)

## 7. Interfaces & Events
Commands/queries exposed, events published and consumed (names per CRC-EVT-002).

## 8. AI Integration Points
Which AI Agents assist, at which autonomy level, with which gates.

## 9. Audit, Security & Compliance
Audit/decision records, permissions, PII classes, regulatory hooks.

## 10. Success Metrics
Leading and lagging indicators with targets (hypotheses to baseline) and measurement method.

## 11. Open Questions
`[Owner] [Blocking|Non-blocking]` question.

## 12. Dependencies & Phasing
What must exist first; what can run in parallel; release slicing. No step-by-step implementation plans.
