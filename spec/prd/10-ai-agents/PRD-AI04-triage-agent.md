# PRD-AI04 — Claim Triage & Prioritization AI Agent

| Field | Value |
|---|---|
| ID | PRD-AI04 |
| Domain | AI Agents |
| Tier | **PARALLEL** (runs on F07) |
| Status | Draft v0.1 |
| Owner | Claims Workflow + AI Platform (TBD) |
| Default autonomy | **L2 Assist** — recommendations are *input facts* to C03 triage rules; rules and humans decide |
| Depends on | F07, C02 (claim data), C03 (triage rules, segments), C05 (reserve context), R03 (fraud score as input), A01 (historical outcomes for evals) |
| Key contract rules | CRC-AI-002, 012, 015; CRC-AUD-004 |

## 1. Problem Statement
Triage decides who handles a claim and how fast; mis-triage sends complex injury claims to fast-track or simple glass claims to senior adjusters. Rules capture known patterns, but free-text descriptions and attachments contain signals rules miss. An AI Agent can read the whole FNOL and recommend complexity, severity, priority, and segment with reasons — feeding the rules engine rather than replacing it, and staying fair and explainable.

## 2. Goals
1. Segment recommendation agreement with expert labels ≥ 85%; under-triage (recommending a lower segment than expert) < 3%.
2. Reassignments due to mis-triage −30% vs. rules-only baseline.
3. Priority recommendations improve time-to-first-contact for high-severity claims by 25%.
4. Fairness: no statistically significant disparity across monitored proxy slices beyond agreed tolerance.

## 3. Non-Goals
- Final segment decision (C03 rules + humans).
- Fraud determination (R03 signals; AI05 summaries).
- Setting reserves (only advisory initial reserve range).

## 4. User Stories
- As a **triage specialist**, I want a recommended segment, complexity, severity, and priority with the top reasons drawn from the claim so that I can confirm quickly.
- As a **supervisor**, I want to see when humans override the AI and why, to improve rules and the AI Agent.
- As a **claims manager**, I want high-severity claims surfaced first even when the FNOL form didn't flag injury but the description implies it.
- *Edge:* FNOL text mentions hospital visit but injury field is "no" → recommendation flags the inconsistency with citation.

## 5. AI Agent Definition
| Element | Specification |
|---|---|
| Inputs | Claim registration data, FNOL narrative, document classifications (AI02), coverage summary, fraud score (R03), similar-claim statistics (A01, aggregated) |
| Output schema | `{segment, complexity (1–5), severity (1–5), priority, suggestedSkills[], initialReserveRange?, reasons[{text, sourceRef}], confidence}` |
| Tools | `READ` getClaimSummary, `READ` getDocumentClassifications, `READ` getSimilarClaimStats; output delivered as Recommendation (no PROPOSE/MUTATE) |
| Guardrails | Excluded attributes (protected classes and flagged proxies); reasons must cite claim data; schema validation |
| Escalation | Low confidence → flagged for specialist triage queue |

## 6. Requirements
### P0
- **AI04-P0-01** Recommendation at registration and on material change events, delivered as facts to C03 triage rules.
- **AI04-P0-02** Reasons with source references, visible in the triage UI.
- **AI04-P0-03** Override capture and feedback loop (CRC-AUD-004).
- **AI04-P0-04** Fairness monitoring slices and tests (CRC-AI-015).
### P1
- **AI04-P1-01** Re-prioritization of open inventory daily (supervisor-approved bulk actions).
- **AI04-P1-02** Initial reserve range recommendation evaluated against closed-claim outcomes.
### P2
- **AI04-P2-01** Next-best-action suggestions during handling.

## 7. Eval Suite
| Metric | Threshold to promote |
|---|---|
| Segment agreement with expert labels | ≥ 85% |
| Under-triage rate | < 3% |
| Severity MAE (1–5 scale) | ≤ 0.6 |
| Reason citation validity | ≥ 98% |
| Fairness slice disparity | Within agreed tolerance |
| Consistency across 5 runs | ≥ 95% same segment |

## 8. Success Metrics
Mis-triage reassignments −30%; high-severity time-to-first-contact −25%; override rate trend and reasons.

## 9. Open Questions
- [Claims] [Blocking] Expert-labeled dataset creation (size, labelers, adjudication).
- [Compliance] [Non-blocking] Fairness proxy slices and tolerance for P&C triage.

## 10. Dependencies & Phasing
Requires C03 triage rules accepting AI facts; shadow first on live registrations; L2 after eval thresholds.
