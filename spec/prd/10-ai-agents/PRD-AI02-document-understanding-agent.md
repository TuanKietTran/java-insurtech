# PRD-AI02 — Document Understanding AI Agent

| Field | Value |
|---|---|
| ID | PRD-AI02 |
| Domain | AI Agents |
| Tier | **PARALLEL** (runs on F07) |
| Status | Draft v0.1 |
| Owner | AI Platform + Claims Operations (TBD) |
| Default autonomy | **L2/L3** — classification L2 (auto-label suggestion; auto-link at L4 only above threshold for non-sensitive types after evals), extraction **L3** (proposals verified by humans) |
| Depends on | F07, F06 (documents, extraction records), C02/C05/V01 (targets), R01 (PHI handling), R03 (consistency signals) |
| Key contract rules | CRC-AI-001, 002, 007, 008, 011; CRC-SEC-012 |

## 1. Problem Statement
Adjusters and vendor managers spend hours reading invoices, estimates, police reports, medical records, and photos to find a handful of values. Manual keying is slow and error-prone, and inconsistencies (an estimate dated before the loss, a photo taken elsewhere) go unnoticed. An AI Agent can classify documents and extract key fields with page-level provenance so humans verify rather than type — as long as originals are never altered and every value points to its source.

## 2. Goals
1. Classification accuracy ≥ 95% across launch document types.
2. Key-field extraction F1 ≥ 0.90 (invoices/estimates), ≥ 0.85 (police reports, medical records) on eval sets.
3. Adjuster time per document −50% on verified extraction flows.
4. 100% of extracted values carry document version + region provenance.

## 3. Non-Goals
- Damage valuation/estimating (vendor platforms).
- Medical necessity or coding decisions.
- Automatic acceptance of invoice amounts (V01 validation + human approval decide).

## 4. User Stories
- As an **adjuster**, I want uploaded documents classified and linked to the right claim line with key values pre-extracted and highlighted on the page.
- As a **vendor manager**, I want invoice line items extracted into structured lines for rate validation (V01).
- As an **SIU analyst**, I want inconsistencies flagged (dates, locations, mismatched VINs) as signals.
- *Edge:* low-quality scan → "unreadable" classification with request for re-upload (F06 document request).
- *Edge:* PHI document → processed only via PHI-approved model routes; extraction visible only to PHI-permitted roles.

## 5. AI Agent Definition
| Element | Specification |
|---|---|
| Inputs | Document version (image/PDF/text), provenance, claim context summary (minimal) |
| Output schema | `{documentType, confidence, suggestedLinks[], extractions[{field, value, confidence, page, bbox|span}], inconsistencies[]}` |
| Tools | `READ` getDocumentVersion (F06), `READ` getClaimContext (C02, minimal), `PROPOSE` recordExtraction (F06), `PROPOSE` linkDocument (F06), `MUTATE` (L4 allowlist candidate) autoLinkDocument for non-sensitive types ≥ threshold, `PROPOSE` raiseSignal (R03) |
| Guardrails | Never modify originals; injection detection on document text; PHI route enforcement; output schema per document type |
| Escalation | Confidence below field threshold → verification queue; unreadable → document request work item |

## 6. Requirements
### P0
- **AI02-P0-01** Classification taxonomy aligned with F06 document types per LOB.
- **AI02-P0-02** Extraction schemas per document type (invoice, repair estimate, police report, medical bill/record summary, ID document, proof of ownership).
- **AI02-P0-03** Verification UI integration (side-by-side with highlights) via F06 extraction records; corrections captured as feedback.
- **AI02-P0-04** Inconsistency checks (dates vs. loss date, VIN/plate vs. policy risk object, EXIF vs. reported location/time) → R03 signals.
- **AI02-P0-05** PHI-aware routing and access.
### P1
- **AI02-P1-01** Photo damage-area labeling (parts, severity bands) as advisory data.
- **AI02-P1-02** Email attachment triage and auto-link suggestions (F06-P1-03).
### P2
- **AI02-P2-01** Video frame extraction for evidence.

## 7. Eval Suite
| Metric | Threshold to promote |
|---|---|
| Classification accuracy | ≥ 95% |
| Field F1 (invoice/estimate) | ≥ 0.90 |
| Field F1 (police/medical) | ≥ 0.85 |
| Provenance completeness | 100% |
| Injection red-team (documents with embedded instructions) | 0 tool misuse |
| Auto-link precision (for L4 candidate types) | ≥ 99.5% |

## 8. Success Metrics
Time per document −50%; verification correction rate trending down; inconsistency signals with SIU acceptance tracked (R03).

## 9. Open Questions
- [Legal] [Blocking] PHI-approved model providers and regions.
- [Claims] [Non-blocking] Launch document types priority by LOB.

## 10. Dependencies & Phasing
Requires F06 extraction API and F07 gateway; start in shadow against already-keyed documents to build eval sets.
