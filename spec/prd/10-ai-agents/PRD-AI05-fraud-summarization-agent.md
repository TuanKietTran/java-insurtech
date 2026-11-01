# PRD-AI05 — Fraud Signal Summarization AI Agent

| Field | Value |
|---|---|
| ID | PRD-AI05 |
| Domain | AI Agents |
| Tier | **PARALLEL** (runs on F07) — high-stakes agent: internal only, grounding blocking |
| Status | Draft v0.1 |
| Owner | SIU + AI Platform (TBD) |
| Default autonomy | **L2 Assist** — internal SIU/adjuster audience only |
| Depends on | F07, R03 (signals, graph, screening), C02/C03 (claim context, notes), F06 (documents), S01 |
| Key contract rules | CRC-AI-002, 008, 015; INV-R03-1, INV-R03-4 |

## 1. Problem Statement
Fraud signals arrive as scattered rule hits, scores, graph links, and document anomalies. Investigators spend significant time assembling them into a coherent picture before deciding whether to open a case, and adjusters often don't understand what a flag means or what to verify. An AI Agent can summarize the signals into a factual, cited narrative with suggested verification steps — strictly internal, non-accusatory, and never a fraud determination.

## 2. Goals
1. SIU referral review time −40%.
2. Summary factual accuracy ≥ 95% (expert-graded); 0 uncited assertions.
3. Adjuster understanding: ≥ 80% of surveyed adjusters find summaries actionable.
4. 0 summaries surfaced to claimant- or vendor-facing channels.

## 3. Non-Goals
- Fraud determination, claim denial, or payment hold decisions.
- Generating new signals beyond restating and correlating existing ones (new signals come from R03 pipelines).
- External communication content.

## 4. User Stories
- As an **SIU investigator**, I want a one-page summary of why a claim was referred, with each point linked to the signal and evidence, and suggested investigative steps.
- As an **adjuster**, I want a neutral explanation of a fraud indicator and what to verify (e.g., "confirm repair shop location; photos' metadata indicate a different city").
- *Edge:* signals conflict (high score but strong legitimate explanation in notes) → summary presents both with citations.

## 5. AI Agent Definition
| Element | Specification |
|---|---|
| Inputs | R03 signals for subject, graph neighborhood (tokenized identifiers), relevant claim facts, document inconsistencies (AI02) |
| Output schema | `{summary, points[{statement, evidenceRefs[]}], suggestedVerificationSteps[], counterIndicators[], confidence}` |
| Tools | `READ` getSignals, `READ` getGraphNeighborhood (R03, SIU permission), `READ` getClaimFacts, `READ` getDocumentInconsistencies; no PROPOSE/MUTATE |
| Guardrails | Non-accusatory language policy (no "fraudster", "lied"); grounding enforced; output visibility class `SIU_INTERNAL` or `ADJUSTER_INTERNAL`; excluded attributes per CRC-AI-015 |
| Escalation | Insufficient evidence → states so; never extrapolates |

## 6. Requirements
### P0
- **AI05-P0-01** SIU summary on referral and on demand.
- **AI05-P0-02** Adjuster-facing neutral explanation variant (fewer details, no graph identities).
- **AI05-P0-03** Grounding with evidence links; counter-indicators section mandatory.
- **AI05-P0-04** Visibility enforcement (never in customer/vendor projections; X01/X03 BFF tests).
### P1
- **AI05-P1-01** Case narrative drafting for SIU reports (L3, investigator edits).
### P2
- **AI05-P2-01** Pattern summaries across rings for SIU managers.

## 7. Eval Suite
| Metric | Threshold to promote |
|---|---|
| Factual accuracy (expert) | ≥ 95% |
| Uncited assertions | 0 |
| Language-policy violations | 0 in red-team set |
| Counter-indicator inclusion when present | ≥ 95% |

## 8. Success Metrics
Referral review time −40%; investigator satisfaction; no leakage of SIU content to external channels.

## 9. Open Questions
- [Legal] [Blocking] Discoverability and retention of AI summaries in litigation (privilege considerations).
- [SIU] [Non-blocking] Summary format standard.

## 10. Dependencies & Phasing
Requires R03 signals and graph; shadow on historical referrals for evals.
