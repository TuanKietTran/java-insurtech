# PRD-AI06 — Communication Drafting AI Agent

| Field | Value |
|---|---|
| ID | PRD-AI06 |
| Domain | AI Agents |
| Tier | **PARALLEL** (runs on F07) |
| Status | Draft v0.1 |
| Owner | Communications Platform + AI Platform (TBD) |
| Default autonomy | **L3 Act-with-approval** (drafts become Proposals → human approves/edits → X04 sends). **L4** only for allowlisted acknowledgements after eval thresholds. Denial/ROR/cancellation/adverse-action content: template-locked, AI may draft only permitted explanatory regions, always human-approved (CRC-AI-002) |
| Depends on | F07, X04 (templates, sending, threads), F06 (templates with AI-editable regions), C02/C03/C05/V01/B01 (context), S01 (guidelines) |
| Key contract rules | CRC-AI-001, 002, 007, 013; INV-X04-4 |

## 1. Problem Statement
Adjusters write dozens of messages a day — status updates, document requests, vendor instructions, internal handoffs, subrogation demands — and regulated letters with strict content. Writing them is slow; quality and tone vary; required elements get missed. An AI Agent can draft clear, accurate, on-tone communications from the claim's facts and approved templates, leaving humans to review and send, while hard rules keep regulated content locked.

## 2. Goals
1. Drafting time per communication −60%.
2. Approval without edits ≥ 50% of drafts after 3 months; edit distance trending down.
3. 0 sent communications containing prohibited content (promises, liability admissions, coverage statements not from approved positions).
4. Readability: customer drafts at configured reading level (e.g., ≤ grade 8) in ≥ 95% of cases.

## 3. Non-Goals
- Autonomous sending of any adverse or regulated communication.
- Marketing content.
- Negotiation strategy decisions (drafts reflect the adjuster's decided position).

## 4. User Stories
- As an **adjuster**, I want a draft status update to the claimant summarizing what's happened and what's next, in their language, so that I can send it in one click after review.
- As an **adjuster**, I want a document request draft listing exactly what's outstanding with due dates.
- As an **adjuster**, I want a draft vendor instruction for an inspection assignment.
- As a **subrogation specialist**, I want a draft demand letter populated from payments and liability evidence.
- As a **supervisor**, I want large-loss summaries for internal reporting drafted from the claim timeline.
- *Edge:* claimant represented → draft addressed to attorney per routing rule; no direct contact content.
- *Edge:* draft requested for a denial → only the explanatory region is drafted; clause citations and rights language come from the approved template and C04 position.

## 5. AI Agent Definition
| Element | Specification |
|---|---|
| Inputs | Communication purpose, template (with editable regions), recipient & routing, claim/policy context (minimal facts), recent timeline, language & reading level |
| Output schema | `{templateId, regions{regionId: text}, citations[], language, readingLevel, warnings[]}` |
| Tools | `READ` getClaimTimelineSummary, `READ` getOutstandingRequests, `READ` getTemplate, `PROPOSE` createCommunicationDraft (X04), `MUTATE` (L4 allowlist) sendAcknowledgement |
| Guardrails | Prohibited-content classifier (coverage promises, liability admissions, payment guarantees, discriminatory language); facts must cite claim records; template-locked regions immutable; AI disclosure tag where required |
| Escalation | Missing facts → draft with placeholders and warnings; never invents |

## 6. Requirements
### P0
- **AI06-P0-01** Drafting for launch purposes: claimant status update, document request, vendor instruction, internal handoff note, acknowledgement.
- **AI06-P0-02** Template-region constraints and locked content enforcement.
- **AI06-P0-03** Proposal review UX (approve/edit/reject) with diff capture into feedback (F07-P0-05).
- **AI06-P0-04** Prohibited-content guardrail and reading-level check.
- **AI06-P0-05** Representation-aware addressing via X04 routing.
### P1
- **AI06-P1-01** Subrogation demand letters and large-loss reports.
- **AI06-P1-02** Translation drafts for launch languages with human review.
- **AI06-P1-03** Reply suggestions in conversation threads.
### P2
- **AI06-P2-01** Proactive status-update suggestions from deadline engine (C02).

## 7. Eval Suite
| Metric | Threshold to promote |
|---|---|
| Factual accuracy vs. claim record | ≥ 98% |
| Prohibited-content violations (red-team) | 0 |
| Required-element coverage per purpose | ≥ 99% |
| Reading-level compliance | ≥ 95% |
| L4 acknowledgement: zero-edit approval rate in L3 period | ≥ 98% before L4 |

## 8. Success Metrics
Drafting time −60%; zero-edit approval ≥ 50%; complaint rate about communications trend down.

## 9. Open Questions
- [Legal] [Blocking] AI disclosure requirements for customer communications per jurisdiction.
- [Governance] [Non-blocking] Approval authority for promoting acknowledgements to L4.

## 10. Dependencies & Phasing
Requires X04 templates with editable regions and F07 proposals; start with internal notes and vendor instructions (lower risk), then customer updates.
