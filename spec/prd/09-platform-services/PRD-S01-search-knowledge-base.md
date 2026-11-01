# PRD-S01 — Search & Knowledge Base

| Field | Value |
|---|---|
| ID | PRD-S01 |
| Domain | Platform Services |
| Tier | **PARALLEL** |
| Status | Draft v0.1 |
| Owner | Platform Search (TBD) |
| Depends on | F03 (events for indexing), F01 (permission filtering), F06 (document text), P01 (forms & editions), R01 (classification, erasure propagation) |
| Consumed by | C01 (policy lookup), C02/C03 (claim search), X01–X03, AI03, AI07, AI05, U01, all workbenches |
| Key contract rules | CRC-SEC-001; CRC-PER-011; CRC-AI-008; CRC-API-008 |

## 1. Problem Statement

Operators lose time finding things: the policy for a caller who only knows their plate number, the claim mentioned in an email, the right edition of a form, the handling guideline for a peril in a jurisdiction. And AI Agents that answer coverage questions are only as good as their retrieval: retrieving the wrong form edition produces confidently wrong answers. The platform needs permission-aware operational search over entities and documents, plus a governed knowledge base (policy wordings by edition, guidelines, regulations) with hybrid retrieval and precise citation anchors.

## 2. Goals
1. **Findability**: ≥ 95% of operational lookups succeed in the first query (telemetry on refinements).
2. **Freshness**: index lag p95 < 5 s after commit.
3. **Correct scoping**: 0 search results returned outside the principal's permissions or tenant.
4. **Retrieval quality**: for AI03/AI07, the correct form edition is retrieved for the policy in ≥ 99% of eval cases; recall@5 for relevant clauses ≥ 90%.
5. **Erasure propagation**: erased/disposed data removed from indexes within 24 hours.

## 3. Non-Goals
- **System of record** — indexes are rebuildable projections (CRC-PER-011).
- **Web search or open internet retrieval** for AI Agents.
- **Enterprise intranet search.**

## 4. Personas & User Stories
- As a **CSR**, I want to find a policy by any of name, phone, address, VIN/plate, or policy number with typo tolerance.
- As an **adjuster**, I want to search across my claims, parties, and documents (full-text in notes and documents I'm allowed to see).
- As a **coverage specialist**, I want to search policy wordings by form number/edition and see differences between editions.
- As a **knowledge manager**, I want to publish claims handling guidelines with owners and review dates.
- As an **AI Agent developer**, I want retrieval scoped to "forms on this coverage snapshot" with chunk anchors for citations.
- *Edge:* SIU notes must never appear for non-SIU users, even as snippets.

## 5. Domain Model
| Entity | Notes |
|---|---|
| **Entity Index** | policies, terms, parties, claims, claim lines, vendors, documents (metadata) — tenant-partitioned, with ACL attributes |
| **Content Index** | document text (F06 OCR/text versions), notes (with visibility class) |
| **Knowledge Item** | type (`POLICY_FORM`, `UW_GUIDELINE`, `CLAIMS_GUIDELINE`, `REGULATION`, `FAQ`, `CLAIM_PRECEDENT_SUMMARY`), applicability (product, jurisdiction, LOB, edition, effective period), owner, review date, status |
| **Chunk** | knowledge item version, section path (e.g., form → section → paragraph IDs), text, embeddings, anchor ID for citation |

**Invariants**
- INV-S01-1: Every query is executed with the principal's tenant and ACL filters applied in the engine (not post-filtered in clients).
- INV-S01-2: Policy-form retrieval for a policy is constrained to the editions on its coverage snapshot unless the caller explicitly requests cross-edition comparison.
- INV-S01-3: Every chunk has a stable anchor that resolves to an exact document version and span.

## 6. Requirements
### P0
- **S01-P0-01** Event-driven indexing of entities and documents with permission attributes; rebuild tooling.
- **S01-P0-02** Operational search APIs (POST search, CRC-API-008) with fuzzy matching, phonetic names, normalized identifiers (VIN, plate, phone, address).
- **S01-P0-03** Permission-aware filtering via F01 attributes (visibility classes, relationship grants).
- **S01-P0-04** Knowledge base with lifecycle (draft/review/published/retired), applicability metadata, versioning.
- **S01-P0-05** Hybrid retrieval (lexical + vector) API for AI Agents with filters by applicability and snapshot form editions; returns anchors and scores.
- **S01-P0-06** Erasure/disposal propagation from R01 events.
- **S01-P0-07** Retrieval eval datasets & metrics integrated with F07 eval harness.
### P1
- **S01-P1-01** Form edition diff view.
- **S01-P1-02** Anonymized past-claim precedent summaries (privacy-reviewed) for adjuster guidance.
- **S01-P1-03** Saved searches and alerts.
### P2
- **S01-P2-01** Multilingual retrieval across translated wordings.
- **S01-P2-02** Graph search over entity resolution (shared with R03).

## 7. Interfaces & Events
- APIs: `POST /v1/search/entities`, `POST /v1/search/content`, `POST /v1/retrieval/query`, `/v1/knowledge-items`.
- **Consumed**: integration events from all contexts, `documents.document.available.v1`, `product.form.edition-published.v1`, `privacy.subject-key.destroyed.v1`, `privacy.retention.disposal-approved.v1`.
- **Published**: `search.knowledge-item.published.v1`, `search.index.rebuild-completed.v1`.

## 8. AI Integration Points
- Retrieval backbone for AI03, AI05, AI07 (and AI06 template guidance); citation anchors satisfy CRC-AI-008.

## 9. Audit, Security & Compliance
- Query logs (without PII in logs) for abuse detection; sensitive content indexes encrypted; knowledge publication audited.

## 10. Success Metrics
| Metric | Type | Target (hypothesis) |
|---|---|---|
| First-query success | Leading | ≥ 95% |
| Index lag p95 | Leading | < 5 s |
| Out-of-scope results | Lagging | 0 |
| Correct-edition retrieval (eval) | Leading | ≥ 99% |
| Clause recall@5 (eval) | Leading | ≥ 90% |

## 11. Open Questions
- [Engineering] [Blocking] Search engine and vector store choice; per-tenant index partitioning strategy (ADR).
- [Legal] [Non-blocking] Use of past-claim summaries for guidance (privacy basis).

## 12. Dependencies & Phasing
- Parallel; needs F03 events and F06 text versions. Knowledge base and retrieval must reach eval thresholds before AI03/AI07 leave shadow.
