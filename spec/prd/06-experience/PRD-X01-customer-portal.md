# PRD-X01 — Customer Self-Service Portal

| Field | Value |
|---|---|
| ID | PRD-X01 |
| Domain | Customer & Producer Portals |
| Tier | **PARALLEL** |
| Status | Draft v0.1 |
| Owner | Digital Experience (TBD) |
| Depends on | F01 (customer auth, relationship grants), P02 (policy views, self-service endorsements), C01 (FNOL), C02 (status mapping), C05 (payment status), B01 (billing, payments), F06 (documents), X04 (preferences), R01 (consent, DSR), AI03/AI07 (assistants) |
| Consumed by | — |
| Key contract rules | CRC-ARCH-011 (BFF has no domain logic); CRC-SEC-006, 009; CRC-API-008; CRC-AI-013 |

## 1. Problem Statement

Policyholders and claimants call the insurer for things they could do themselves — get an ID card, check claim status, pay a bill, upload a photo — because portals are fragmented or show internal jargon. Every avoidable call costs money and every opaque status erodes trust during the most stressful moments. The platform needs a single, accessible web portal that exposes policies, billing, and claims in plain language, built entirely on domain APIs.

## 2. Goals
1. **Containment**: −30% inbound calls for status, documents, and payments among portal-registered customers (hypothesis).
2. **Adoption**: ≥ 40% of active policyholders registered within 12 months.
3. **Claim transparency**: ≥ 70% of claimants with portal access view status at least once; status-inquiry calls per claim −40%.
4. **Satisfaction**: task completion rate ≥ 90% for top tasks; CSAT ≥ 4.3/5.
5. **Accessibility**: WCAG 2.2 AA conformance on all flows.

## 3. Non-Goals
- **New-business direct sales/quoting** for unauthenticated prospects (separate D2C initiative; may reuse P02 APIs).
- **Domain logic in the portal** (CRC-ARCH-011).
- **Native mobile** (X03).

## 4. Personas & User Stories
- As a **policyholder**, I want to view my coverages, limits, deductibles, and documents (ID cards, declarations) and download them.
- As a **policyholder**, I want to pay my bill, manage autopay, and see my payment history.
- As a **policyholder**, I want to make simple changes (address, add/remove vehicle or driver) with an instant price and documents, or a clear "under review" status.
- As a **claimant**, I want to report a claim, track it on a timeline with plain-language status and next steps, upload requested documents, message my adjuster, and see payments.
- As a **third-party claimant** without a policy, I want secure access to my claim only.
- As a **customer**, I want to manage communication preferences and consents, and request a copy of my data.
- As a **customer**, I want to ask questions about my policy and get clear answers with references (AI07/AI03, disclosed as AI).
- *Edge:* a claim under SIU review shows a neutral status; SIU is never revealed.
- *Edge:* a self-service change triggers UW referral → status "under review" with expected timeframe.

## 5. Scope & Owned Entities
- Owns: portal UX, `bff-customer`, view composition, session/UX preferences. No business entities.
- Customer-facing claim statuses come from C02's status mapping; billing statuses from B01; policy statuses from P02.

## 6. Requirements
### P0
- **X01-P0-01** Registration & account linking (policy number + verification factors), passwordless login (F01), multi-policy household view.
- **X01-P0-02** Policy views: coverages, limits, deductibles, risk objects, documents (F06), ID cards.
- **X01-P0-03** Billing: balance, invoices, payment (PSP hosted fields), autopay, history (B01).
- **X01-P0-04** Self-service endorsements allowed by product rules (P02-P1-05), with price preview and documents.
- **X01-P0-05** FNOL (C01) with save/resume and uploads.
- **X01-P0-06** Claim tracker: timeline, plain-language status, next steps, outstanding document requests, payments, adjuster contact, secure messaging.
  *Acceptance:* Given an outstanding document request, the claimant sees it with due date and can fulfil it via upload; the request status updates in C03/F06 within 30 s.
- **X01-P0-07** Preferences & consents (X04/R01), DSR initiation (R01).
- **X01-P0-08** Accessibility, localization, responsive design.
### P1
- **X01-P1-01** AI assistant for policy questions and claim status (AI07/AI03 patterns; disclosed; escalate to human).
- **X01-P1-02** Appointment scheduling for inspections (V01).
- **X01-P1-03** Payment method selection for claim payouts (C05-P1-04).
- **X01-P1-04** Proactive notifications center (in-app inbox).
### P2
- **X01-P2-01** Delegated access (family member, public adjuster) (F01-P2-02).
- **X01-P2-02** Cross-sell/endorsement suggestions (AI07) subject to licensing/disclosure rules.

## 7. Interfaces & Events
- BFF composes: P02, B01, C01, C02, C05, F06, X04, R01 APIs. Subscribes to status-change events for push to the in-app inbox (P1).

## 8. AI Integration Points
- AI07 (policy wording explanation) and AI03 customer mode (P1) — grounded, disclosed, no coverage promises, human handoff.

## 9. Audit, Security & Compliance
- ASVS L2 (L3 for payment/bank flows); bot protection; session security; no PII in URLs; customer actions audited through domain APIs.

## 10. Success Metrics
| Metric | Type | Target (hypothesis) |
|---|---|---|
| Registered active policyholders | Lagging | ≥ 40% at 12 months |
| Status-inquiry calls per claim | Lagging | −40% |
| Top-task completion | Leading | ≥ 90% |
| CSAT | Lagging | ≥ 4.3/5 |
| WCAG 2.2 AA violations (automated + audit) | Leading | 0 critical |

## 11. Open Questions
- [Product] [Blocking] Which self-service endorsements at launch per product.
- [Design] [Non-blocking] Shared design system with X02/X03.

## 12. Dependencies & Phasing
- Can start once BFF-consumed API contracts are frozen (P02 read, B01 payments, C01 intake, C02 status). UI work parallel to domain build using contract mocks.
