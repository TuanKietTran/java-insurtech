# PRD-AI01 — FNOL Intake & Structured Extraction AI Agent

| Field | Value |
|---|---|
| ID | PRD-AI01 |
| Domain | AI Agents |
| Tier | **PARALLEL** (runs on F07) |
| Status | Draft v0.1 |
| Owner | Claims Intake + AI Platform (TBD) |
| Default autonomy | **L2 Assist** (chat collects; reporter/CSR confirms); extraction from email/documents **L3** (proposal → CSR confirm) |
| Depends on | F07, C01 (loss report schema & APIs), P01 (LOB question sets), S01 (policy lookup), F06 |
| Key contract rules | CRC-AI-001…014; CRC-SEC-006 |

## 1. Problem Statement
Reporters describe losses in their own words — in chat, on the phone, in emails — while adjusters need structured, complete data. Forms are rigid and abandoned; free text is incomplete and inconsistent. An AI Agent can hold a natural conversation, ask the right follow-ups for the LOB, and map what it hears into the canonical loss report with sources, but only if it never invents facts, never promises coverage, and escalates emergencies instantly.

## 2. Goals
1. Field-level extraction F1 ≥ 0.92 on the eval set for required FNOL fields.
2. Unsupported (hallucinated) field values < 1% of extracted fields.
3. Chat FNOL completion rate ≥ 75% of started conversations (hypothesis).
4. Emergency/injury signals detected with recall ≥ 99% on eval set; escalated to a human within 30 s.

## 3. Non-Goals
- Coverage statements, claim acceptance, or settlement discussion.
- Policy servicing in the same conversation (hand off to other flows).
- Voice telephony integration (P2 via C01-P2-02).

## 4. User Stories
- As a **policyholder in chat**, I want to describe what happened and be asked only relevant follow-ups so that reporting feels quick.
- As a **CSR**, I want an email or call transcript turned into a draft loss report with each field linked to the sentence it came from so that I can confirm quickly.
- As a **reporter**, I want to review a plain summary before submitting so that I can fix mistakes.
- *Edge:* reporter mentions someone is hurt → immediate safety guidance and live-human handoff.
- *Edge:* reporter asks "am I covered?" → neutral response explaining an adjuster will review coverage.

## 5. AI Agent Definition
| Element | Specification |
|---|---|
| Inputs | Conversation turns / transcript / email text + attachments metadata; LOB question set (P01); policy context if authenticated (summary only, no PII beyond need) |
| Output schema | Canonical `LossReport` draft (C01 schema) with per-field `{value, confidence, sourceSpan}` + `missingRequiredFields[]` + `safetyFlags[]` |
| Tools | `READ` lookupPolicyCandidates (S01, scoped), `READ` getQuestionSet (P01), `PROPOSE` saveLossReportDraft (C01), `MUTATE`(L4 allowlist candidate) none at launch |
| Guardrails | No coverage/liability statements; no promises of payment or timing beyond configured SLAs; injection detection on emails/attachments; PII redaction profile for provider; AI disclosure in chat (CRC-AI-013) |
| Escalation | Safety flags → immediate human; confidence < threshold on required field → ask follow-up (chat) or highlight (CSR); 3 failed schema validations → CSR work item |
| Budgets | ≤ 40 turns, ≤ configured tokens/cost per conversation |

## 6. Requirements
### P0
- **AI01-P0-01** Conversational intake driven by P01 question sets with adaptive follow-ups.
- **AI01-P0-02** Extraction with source spans and confidence per field; never fills a field without a source.
- **AI01-P0-03** Summary confirmation before submit; reporter edits captured as feedback.
- **AI01-P0-04** Safety/injury detection and human handoff.
- **AI01-P0-05** Prohibited-statement guardrail (coverage, liability, fault, payment promises).
- **AI01-P0-06** Email/transcript extraction to proposals for CSR (L3).
### P1
- **AI01-P1-01** Multilingual intake for launch languages.
- **AI01-P1-02** Photo-aware follow-ups (ask for missing angles) with AI02.
### P2
- **AI01-P2-01** Real-time voice transcript assist for CSRs.

## 7. Eval Suite
| Metric | Threshold to promote |
|---|---|
| Required-field F1 | ≥ 0.92 |
| Unsupported field rate | < 1% |
| Injury/safety recall | ≥ 99% |
| Prohibited-statement violations | 0 in red-team set |
| Consistency (same transcript, 5 runs) field agreement | ≥ 97% |
| Slices: LOB, language, channel | No slice > 5 pts below overall |

## 8. Success Metrics
Chat completion ≥ 75%; CSR handling time for email FNOL −40%; FNOL completeness (C01) uplift +10 pts for AI-assisted channels.

## 9. Open Questions
- [Legal] [Blocking] Chat disclosure and recording consent per jurisdiction.
- [Claims] [Non-blocking] Which LOBs launch in chat first.

## 10. Dependencies & Phasing
Shadow (L1) on CSR-entered FNOLs to build evals → L2 chat for one LOB → expand.
