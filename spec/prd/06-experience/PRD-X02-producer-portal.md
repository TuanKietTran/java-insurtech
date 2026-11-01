# PRD-X02 — Producer (Agent/Broker) Portal

| Field | Value |
|---|---|
| ID | PRD-X02 |
| Domain | Customer & Producer Portals |
| Tier | **PARALLEL** |
| Status | Draft v0.1 |
| Owner | Distribution Experience (TBD) |
| Depends on | F01 (producer orgs, hierarchy, binding authority, licensing), P02 (quote/bind/service), P03 (rating), U01 (submissions, referrals), B01 (billing status, agency statements), B02 (commission statements), C01/C02 (client claims), F06, S02 (agency management system and comparative rater integrations) |
| Consumed by | — |
| Key contract rules | CRC-ARCH-011; CRC-SEC-001; F01 relationship grants |

## 1. Problem Statement

Producers choose carriers that are easy to do business with: fast quotes, clear referrals, self-service servicing, and visibility into their clients' billing and claims. Clunky portals, re-keying from agency systems, and opaque referral status push business to competitors. The platform needs a producer portal and APIs that cover quoting through servicing within each producer's authority and book of business.

## 2. Goals
1. **Quote speed**: personal-lines quote in < 5 minutes of producer time; small commercial < 15 minutes (hypothesis).
2. **Self-service servicing**: ≥ 70% of producer service requests completed without carrier staff.
3. **Referral transparency**: 100% of referred quotes show status, reasons, and required information.
4. **Producer satisfaction** (NPS) baseline then +15 points.

## 3. Non-Goals
- **Agency management system** (integrate with AMS via S02).
- **Producer licensing system of record** (F01/S02 attributes).
- **Domain logic in portal** (CRC-ARCH-011).

## 4. Personas & User Stories
- As a **producer**, I want to quote multiple products for a client, compare options, and bind within my binding authority.
- As a **producer**, I want to see referral reasons and upload requested information so that UW can decide quickly.
- As a **producer**, I want to service policies (endorsements, cancellation requests, certificates/ID cards) and see my clients' billing status and claim status (limited view).
- As an **agency principal**, I want to manage my agency's users, their book visibility, and see commission statements.
- As a **producer**, I want my AMS or comparative rater to submit quotes via API so that I don't re-key.
- *Edge:* producer not licensed in the risk's jurisdiction → quote allowed for information only, bind blocked with explanation.

## 5. Scope & Owned Entities
- Owns: `bff-producer`, producer UX, saved quote workspaces (UI state). No business entities.

## 6. Requirements
### P0
- **X02-P0-01** Producer login (OIDC), agency hierarchy-aware book of business (F01).
- **X02-P0-02** Quote & bind flows (P02/P03), multi-option comparison, proposal PDFs, binding-authority enforcement.
- **X02-P0-03** Submissions & referrals (U01) with status, reasons, requests for information, and messaging with UW.
- **X02-P0-04** Servicing: endorsements, cancellation requests, reinstatement requests, documents.
- **X02-P0-05** Client billing view (B01) and agency-bill statements/remittance.
- **X02-P0-06** Client claims view (C02 limited projection) and FNOL on behalf of clients (C01).
- **X02-P0-07** Commission statements (B02).
- **X02-P0-08** Agency user administration (F01 delegated admin).
### P1
- **X02-P1-01** AMS integration (download of policy/billing data; ACORD standards via S02).
- **X02-P1-02** Comparative rater API intake.
- **X02-P1-03** Renewal pipeline view with at-risk renewals.
- **X02-P1-04** AI07 endorsement suggestions/explanations for producers (L2).
### P2
- **X02-P2-01** Producer analytics (book loss ratio, retention).
- **X02-P2-02** Co-branded client portal links.

## 7. Interfaces & Events
- BFF composes P02, P03, U01, B01, B02, C01, C02, F06 APIs; external producer APIs exposed via S02 gateway with partner auth.

## 8. AI Integration Points
- AI07 suggestions (informational, producer decides); submission summaries visible to producer only where permitted.

## 9. Audit, Security & Compliance
- Binding authority and licensing enforced in domain (P02/F01); producer actions audited; producer sees only book per relationship grants.

## 10. Success Metrics
| Metric | Type | Target (hypothesis) |
|---|---|---|
| Producer time per personal-lines quote | Leading | < 5 min |
| Self-service servicing rate | Lagging | ≥ 70% |
| Referred quotes with visible status/reasons | Leading | 100% |
| Producer NPS | Lagging | +15 vs. baseline |

## 11. Open Questions
- [Distribution] [Blocking] Launch distribution model (independent agencies, captive producers, brokers, MGAs).
- [Product] [Non-blocking] Which AMS integrations first.

## 12. Dependencies & Phasing
- Parallel to domain build using frozen API contracts; bind/service flows go live as P02 capabilities land.
