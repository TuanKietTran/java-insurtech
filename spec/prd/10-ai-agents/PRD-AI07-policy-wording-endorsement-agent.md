# PRD-AI07 — Policy Wording Explanation & Endorsement Suggestion AI Agent

| Field | Value |
|---|---|
| ID | PRD-AI07 |
| Domain | AI Agents |
| Tier | **PARALLEL** (runs on F07) — high-stakes agent: grounding blocking |
| Status | Draft v0.1 |
| Owner | Policy Platform + Distribution + AI Platform (TBD) |
| Default autonomy | **L2 Assist** — explanations to customers/producers; endorsement *suggestions* shown to licensed producers (or as informational content to customers where permitted). Never quotes, binds, or changes a policy |
| Depends on | F07, S01 (form chunks), P01 (endorsement definitions & eligibility), P02 (policy versions, snapshot), P03 (indicative pricing via quote API, P1), X01/X02 |
| Key contract rules | CRC-AI-002, 008, 013; CRC-CFG-002; F01 licensing attributes |

## 1. Problem Statement
Policyholders rarely understand their coverage until a claim, when gaps cause disputes and dissatisfaction. Producers lack time to review every policy for gaps at renewal. An AI Agent can explain policy wording in plain language, grounded in the customer's actual forms, and suggest relevant endorsements based on the insured's situation — within sales, licensing, and disclosure rules, and without ever making coverage promises or changing the policy.

## 2. Goals
1. Explanation accuracy ≥ 92% (expert-graded) with 0 uncited policy-specific assertions.
2. Customer comprehension: ≥ 80% "this helped me understand" in in-product surveys (hypothesis).
3. Endorsement suggestion relevance: ≥ 70% of producer-viewed suggestions rated relevant; suggestion-to-quote rate tracked.
4. 0 compliance violations (unlicensed solicitation, misleading statements) in QA review.

## 3. Non-Goals
- Binding, quoting, or applying endorsements (P02 flows with human/producer action).
- Personalized advice that constitutes licensed solicitation to customers where not permitted.
- Explaining claims decisions (C04 letters own that).

## 4. User Stories
- As a **policyholder**, I want to ask "what does my policy cover if a tree falls on my car?" and get a plain-language answer citing my policy, with a note that claims are evaluated on facts.
- As a **policyholder**, I want a plain-language summary of my declarations and key exclusions at purchase and renewal.
- As a **producer**, I want suggestions of endorsements relevant to my client (e.g., based on risk-object attributes or life events) with the reason and eligibility, so that I can have an informed conversation.
- *Edge:* a suggested endorsement isn't eligible in the jurisdiction/product version → never suggested (P01 eligibility rules).
- *Edge:* question outside the policy's scope → explains limits of the answer and offers human contact.

## 5. AI Agent Definition
| Element | Specification |
|---|---|
| Inputs | Policy term/snapshot, question or "summarize" request, audience (customer/producer), language, risk-object attributes (for suggestions), producer licensing attributes |
| Output schema | Explanation: `{answer, citations[], disclaimers[], readingLevel}`; Suggestions: `{endorsements[{code, reason, eligibilityRuleRef, citations[]}], audience}` |
| Tools | `READ` getCoverageSnapshot, `READ` retrieveClauses (S01, snapshot-scoped), `READ` getEligibleEndorsements (P01 rules evaluated for the policy), `READ` (P1) getIndicativePrice (P03 via quote API, producer only) |
| Guardrails | Grounding enforcement; no coverage guarantees; mandatory disclaimer; suggestions only from rule-evaluated eligible endorsements (CRC-AI-012); audience gating by licensing/permissions; AI disclosure (CRC-AI-013) |
| Escalation | Low confidence or complaint signals → offer human (CSR/producer) contact |

## 6. Requirements
### P0
- **AI07-P0-01** Grounded explanations for customers (X01/X03) and producers (X02).
- **AI07-P0-02** Plain-language declarations/key-exclusions summary at issuance/renewal (reviewed template framing).
- **AI07-P0-03** Endorsement suggestions for producers limited to rule-eligible endorsements, with reasons and citations.
- **AI07-P0-04** Audience gating and disclosure rules per jurisdiction.
### P1
- **AI07-P1-01** Indicative price impact for suggestions (producer audience, via quote API).
- **AI07-P1-02** Renewal-change explanations ("what changed and why") from version diffs and rating worksheets (P03).
- **AI07-P1-03** Customer-facing suggestions as informational content where legally permitted.
### P2
- **AI07-P2-01** Life-event–triggered suggestions (move, new driver) with consent.

## 7. Eval Suite
| Metric | Threshold to promote |
|---|---|
| Expert-graded explanation accuracy | ≥ 92% |
| Uncited policy-specific assertions | 0 |
| Ineligible endorsement suggestions | 0 |
| Compliance red-team (solicitation, guarantees) | 0 violations |
| Reading-level compliance (customer) | ≥ 95% |

## 8. Success Metrics
Comprehension survey ≥ 80%; producer relevance ≥ 70%; coverage-misunderstanding complaints trend down; suggestion-to-quote conversion tracked (no target until baseline).

## 9. Open Questions
- [Legal] [Blocking] What constitutes solicitation/advice by AI to customers per jurisdiction; required disclosures.
- [Distribution] [Non-blocking] Producer opt-in and display of suggestions.

## 10. Dependencies & Phasing
Requires S01 snapshot-scoped retrieval and P01 eligibility evaluation; producer audience first, customer explanations second, customer suggestions last.
