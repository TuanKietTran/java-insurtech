# PRD-AI03 — Coverage & Policy Q&A (RAG) AI Agent

| Field | Value |
|---|---|
| ID | PRD-AI03 |
| Domain | AI Agents |
| Tier | **PARALLEL** (runs on F07) — high-stakes agent: grounding is blocking (CRC-AI-008) |
| Status | Draft v0.1 |
| Owner | Claims Core + AI Platform (TBD) |
| Default autonomy | **L2 Assist** (advisory answers to internal users); customer mode P1 with stricter guardrails |
| Depends on | F07, S01 (retrieval constrained to snapshot editions), P02 (CoverageSnapshot), P01 (forms, coverage definitions), C04 (coverage evaluation results), F02 |
| Key contract rules | CRC-AI-002, 008, 012; CRC-AUD-003 |

## 1. Problem Statement
Adjusters, CSRs, and producers constantly need to know what a specific policy says: is this peril excluded, what's the sublimit for jewelry, does the rental coverage have a daily cap. Finding the answer means reading the right edition of several forms and endorsements and reconciling them. A retrieval-grounded AI Agent can answer in seconds with exact citations from the policy's own forms — provided it never answers from general knowledge, never confuses editions, and never presents its answer as a coverage determination.

## 2. Goals
1. Answer correctness ≥ 90% (expert-graded) on the eval set; citation precision ≥ 95%.
2. Uncited assertions blocked: 0 uncited policy-specific claims delivered.
3. "Insufficient information" correctly returned for ≥ 95% of unanswerable questions.
4. Adjuster time to answer wording questions −60%.

## 3. Non-Goals
- Setting coverage positions or drafting denials (C04 + humans).
- Legal advice or case-law interpretation.
- Answering from general insurance knowledge without the policy's forms.

## 4. User Stories
- As an **adjuster**, I want to ask a question about the policy on this claim and get an answer quoting the relevant clauses from the exact form editions on the coverage snapshot.
- As an **adjuster**, I want to see how the deterministic coverage evaluation (C04) relates to the answer, and be warned when they disagree.
- As a **CSR**, I want quick answers to "what's my deductible for glass?" for a caller's policy.
- *Edge:* question requires facts not in the policy (e.g., "was the roof old?") → answer explains what the policy says and which facts are needed.
- *Edge:* endorsement modifies a base form clause → answer reflects the endorsement and cites both.

## 5. AI Agent Definition
| Element | Specification |
|---|---|
| Inputs | Question, subject (policy term or claim → coverage snapshot ID), user role |
| Retrieval | S01 hybrid retrieval restricted to form editions + endorsements on the snapshot, plus coverage schedule (limits/deductibles) from snapshot data |
| Output schema | `{answer, citations[{anchorId, quote}], snapshotId, confidence, needsFacts[], relatedCoverageEvaluationRef?, disagreementWithRules?: bool}` |
| Tools | `READ` getCoverageSnapshot, `READ` retrieveClauses, `READ` getCoverageEvaluation (C04); no PROPOSE/MUTATE tools |
| Guardrails | Grounding check: every policy-specific sentence must map to ≥ 1 citation or be removed; label "Advisory — not a coverage determination"; refuse speculation; injection-safe retrieval |
| Escalation | Low confidence or rule disagreement → suggest coverage specialist review (C03 work item on request) |

## 6. Requirements
### P0
- **AI03-P0-01** Snapshot-scoped retrieval (INV-S01-2) with endorsement precedence handling.
- **AI03-P0-02** Citation rendering with click-through to the exact span (F06/S01 anchors).
- **AI03-P0-03** Grounding enforcement and "insufficient information" behavior.
- **AI03-P0-04** Rule-disagreement detection vs. C04 evaluation (CRC-AI-012) logged as a signal.
- **AI03-P0-05** Feedback capture (helpful / incorrect + correction) into eval datasets.
### P1
- **AI03-P1-01** Customer/producer mode via X01/X02 with simplified language and stricter refusal policy.
- **AI03-P1-02** Cross-edition comparison questions for coverage specialists.
### P2
- **AI03-P2-01** Guideline-aware answers (claims handling guidelines + regulations from S01) clearly separated from policy wording.

## 7. Eval Suite
| Metric | Threshold to promote |
|---|---|
| Expert-graded correctness | ≥ 90% |
| Citation precision / recall | ≥ 95% / ≥ 85% |
| Correct-edition retrieval | ≥ 99% |
| Unanswerable → "insufficient information" | ≥ 95% |
| Uncited policy-specific assertions | 0 |
| Consistency across 5 runs | ≥ 95% semantic agreement |

## 8. Success Metrics
Time to answer −60%; usage by adjusters weekly ≥ 60%; disagreement signals reviewed and reduced over time.

## 9. Open Questions
- [Legal] [Blocking] Customer-facing Q&A permissibility and disclaimers per jurisdiction (P1).
- [Claims] [Non-blocking] Expert grader pool and calibration process.

## 10. Dependencies & Phasing
Requires S01 retrieval meeting its eval thresholds and P01 form chunk anchors. Internal L2 first; customer mode later.
