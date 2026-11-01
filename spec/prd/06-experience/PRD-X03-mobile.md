# PRD-X03 — Mobile Experiences

| Field | Value |
|---|---|
| ID | PRD-X03 |
| Domain | Customer & Producer Portals |
| Tier | **PARALLEL** |
| Status | Draft v0.1 |
| Owner | Digital Experience — Mobile (TBD) |
| Depends on | X01 capabilities via `bff-mobile`, C01 (FNOL), F06 (uploads with provenance), F01 (auth, device binding), X04 (push), B01 (payments) |
| Consumed by | — |
| Key contract rules | CRC-ARCH-011; CRC-SEC-006, 009; F06 provenance requirements |

## 1. Problem Statement

The moment of loss happens away from a desk: at the roadside, in a flooded basement. Mobile is where customers can capture the best evidence (photos with time and location) and get help fastest, and where ID cards and payments are most often needed. A mobile app that merely wraps the website misses device capabilities that improve evidence quality and speed. The platform needs native mobile experiences for customers (and later field adjusters) built on the same domain APIs.

## 2. Goals
1. **Mobile FNOL** completion in < 5 minutes median for auto claims with photos.
2. **Evidence quality**: ≥ 90% of mobile-submitted photos carry device capture metadata and pass quality checks (P1 guided capture).
3. **App adoption**: ≥ 25% of active policyholders with the app installed and logged in within 12 months.
4. **Crash-free sessions** ≥ 99.5%.

## 3. Non-Goals
- **Feature parity with every web flow at launch** — prioritize at-loss and frequent tasks.
- **Telematics/driving-behavior programs** (P2).
- **Domain logic on the device or BFF** (CRC-ARCH-011).

## 4. Personas & User Stories
- As a **policyholder**, I want digital ID cards in my phone wallet so that I can show proof of insurance instantly, even offline.
- As a **policyholder at an accident scene**, I want a guided flow to capture photos, other driver details (scan license/plate), location, and safety guidance.
- As a **claimant**, I want push notifications when my claim status changes or documents are requested.
- As a **policyholder**, I want to pay with a mobile wallet and log in with biometrics.
- *Edge:* no connectivity → capture offline, queue uploads, sync later with original capture timestamps preserved.
- *(P1)* As a **field appraiser**, I want offline inspection workflows with photo capture and estimate upload.

## 5. Scope & Owned Entities
- Owns: iOS/Android apps, `bff-mobile`, device registration for push, offline capture queue (client-side). No business entities.

## 6. Requirements
### P0
- **X03-P0-01** Auth with biometrics/passkeys and device binding (F01).
- **X03-P0-02** Policies, ID cards (wallet passes, offline cache), documents.
- **X03-P0-03** Mobile FNOL (C01) with camera capture, GPS location, time, device metadata recorded as provenance (F06); offline queue with integrity (hash at capture).
- **X03-P0-04** Claim tracker and document request fulfillment.
- **X03-P0-05** Billing & payments with mobile wallets (PSP SDK).
- **X03-P0-06** Push notifications (X04) respecting preferences; deep links to tasks.
- **X03-P0-07** Accessibility (platform accessibility APIs), localization.
### P1
- **X03-P1-01** Guided photo capture with quality checks (blur, framing, coverage of damage areas).
- **X03-P1-02** Roadside assistance and towing request (V01).
- **X03-P1-03** Field adjuster/appraiser app: assignments, offline inspections, estimates upload.
- **X03-P1-04** Device integrity attestation attached to evidence provenance.
### P2
- **X03-P2-01** Telematics and crash detection (consented).
- **X03-P2-02** Live video inspection.

## 7. Interfaces & Events
- `bff-mobile` composes the same APIs as X01; push via X04 channel adapter; uploads directly to F06 resumable endpoints.

## 8. AI Integration Points
- On-upload classification and quality feedback (AI02, L2), chat FNOL (AI01).

## 9. Audit, Security & Compliance
- Certificate pinning, secure storage for tokens, jailbreak/root detection policy, minimal PII cached offline (ID cards only, encrypted).

## 10. Success Metrics
| Metric | Type | Target (hypothesis) |
|---|---|---|
| Median mobile auto FNOL time | Leading | < 5 min |
| Photos with capture metadata | Leading | ≥ 90% |
| App adoption (active policyholders) | Lagging | ≥ 25% at 12 months |
| Crash-free sessions | Leading | ≥ 99.5% |

## 11. Open Questions
- [Engineering] [Blocking] Native vs. cross-platform framework (ADR).
- [Legal] [Non-blocking] Location data consent wording per jurisdiction.

## 12. Dependencies & Phasing
- Parallel; depends on F06 resumable upload API and C01 intake API being frozen.
