# PRD-X04 — Notifications & Communications Engine

| Field | Value |
|---|---|
| ID | PRD-X04 |
| Domain | Cross-cutting — Communications |
| Tier | **PARALLEL** (required-notice delivery evidence is non-negotiable) |
| Status | Draft v0.1 |
| Owner | Communications Platform (TBD) |
| Depends on | F06 (templates, generated documents, proof of mailing), F05 (routing & required-notice rules), R01 (consent, preferences), F03 (events), S02 (email/SMS/push/print-mail providers), F07/AI06 (drafting) |
| Consumed by | P02, B01, B02, C01–C05, V01, U01, X01–X03, F02 |
| Key contract rules | CRC-IDEM-004; CRC-CFG-001, 002; CRC-SEC-006; CRC-AI-002, 013 |

## 1. Problem Statement

Insurers send regulated communications — cancellation notices, denial letters, adverse-action notices — where timing, content, and proof of delivery are legal requirements, alongside service messages customers expect instantly. When each context sends its own emails and letters, required notices go out late or not at all, consent rules are violated, duplicates annoy customers, and nobody can prove what was sent. The platform needs one communications engine with versioned templates, consent enforcement, multi-channel delivery, idempotent sends, and retained proof.

## 2. Goals
1. **Required-notice compliance**: 100% of required notices generated, sent via a compliant channel, and evidenced within the required window.
2. **Consent compliance**: 0 messages sent in violation of channel consent/opt-out (e.g., SMS without consent).
3. **No duplicates**: duplicate sends per business event < 0.01%.
4. **Delivery**: email/SMS delivery rate ≥ 98% to valid addresses; bounce handling triggers alternate channel for required notices.
5. **Template agility**: template changes without code; jurisdiction variants via configuration.

## 3. Non-Goals
- **Marketing campaign management.** Transactional and regulatory communications only (marketing consent respected but campaigns are external).
- **Contact-center telephony.**
- **Free-form AI-sent communications without approval** (AI06 operates under F07 gates).

## 4. Personas & User Stories
- As a **compliance officer**, I want every cancellation notice's content, recipient, channel, send time, and proof of mailing retained and retrievable.
- As a **template manager**, I want to manage templates by type, jurisdiction, language, and effective date with approval.
- As an **adjuster**, I want to send a letter or message from the claim using a template (optionally AI-drafted), and see replies in the claim conversation thread.
- As a **customer**, I want messages on my preferred channel and language, and to opt out of non-required messages.
- *Edge:* claimant represented by an attorney → communications route to the attorney per rule (C02 representation).
- *Edge:* email bounce on a required notice → fallback to print/mail with proof.

## 5. Domain Model & Invariants
| Entity | Notes |
|---|---|
| **Template** | (F06 template) type, purpose (`REQUIRED_NOTICE`, `SERVICE`, `CORRESPONDENCE`), channels, jurisdiction/product/language variants, version, effective dates, approval, data schema, AI-editable regions (for AI06) |
| **Communication Request** | trigger (event/user/AI proposal), recipient party + resolved routing (representative rules), template version, data snapshot, channel plan (primary + fallbacks), dedupe key, required-by deadline |
| **Message** | channel, rendered content ref (F06 document), provider message ID, status (`QUEUED`, `SENT`, `DELIVERED`, `BOUNCED`, `FAILED`, `OPENED`*), timestamps, proof artifacts |
| **Conversation Thread** | per claim/policy/party: outbound + inbound messages (email replies, portal messages, SMS replies) |
| **Preference & Consent** | (R01) per party × channel × purpose |

**Invariants**
- INV-X04-1: A dedupe key (business event × recipient × template purpose) results in at most one communication (CRC-IDEM-004).
- INV-X04-2: Required notices bypass marketing opt-outs but obey legal channel rules; non-required messages honor all opt-outs and quiet hours.
- INV-X04-3: Sent content is retained as an immutable document (F06) with template version and data hash.
- INV-X04-4: Adverse communications (denial, ROR, cancellation, adverse action) require approved templates and human approval when any content is non-template (CRC-AI-002).

## 6. Requirements
### P0
- **X04-P0-01** Event-driven triggers and API-initiated sends; channel plan with fallbacks.
- **X04-P0-02** Template management (versioning, jurisdiction/language variants, approval, effective dates).
- **X04-P0-03** Channels: email, SMS, push, in-app/portal inbox, print & mail via provider (S02) with proof of mailing.
- **X04-P0-04** Consent/preference enforcement (R01), quiet hours, frequency caps for non-required messages.
- **X04-P0-05** Representation & routing rules (attorney, public adjuster, lienholder copies).
- **X04-P0-06** Delivery tracking and bounce/failure handling, fallback for required notices; deadline monitoring with alerts.
  *Acceptance:* Given a cancellation notice with a mailing deadline of day 10, when email bounces on day 2, then print/mail is dispatched automatically and proof is retained before day 10; failure to dispatch by deadline − 2 days raises a P1 alert.
- **X04-P0-07** Conversation threads with inbound routing to claims/policy work queues (C03).
- **X04-P0-08** Retention of sent content and proof (F06) and audit.
### P1
- **X04-P1-01** AI06-drafted correspondence in threads (L3 approval).
- **X04-P1-02** Two-way SMS with keyword handling (STOP/HELP) and intent routing.
- **X04-P1-03** Translation workflows for templates and approved AI translation with review.
- **X04-P1-04** Communication analytics (delivery, read, response times).
### P2
- **X04-P2-01** Messaging apps (e.g., WhatsApp) where permitted.
- **X04-P2-02** Accessible formats (large print, braille) on request.

## 7. Interfaces & Events
- APIs: `/v1/communications` (send request), `/v1/communications/{id}`, `/v1/threads/{id}`, `/v1/templates`.
- **Published**: `notifications.communication.requested.v1`, `…sent.v1`, `…delivered.v1`, `…failed.v1`, `…required-notice-at-risk.v1`, `notifications.inbound-message.received.v1`.
- **Consumed**: domain events that trigger communications (e.g., `policy.term.cancellation-scheduled.v1`, `claims.claim.registered.v1`, `billing.invoice.issued.v1`, `claims.coverage.denial-issued.v1`, `aiharness.proposal.approved.v1`).

## 8. AI Integration Points
- AI06 drafts within template-permitted regions; L3 by default; L4 only for allowlisted low-risk acknowledgements after eval thresholds (F07).
- Disclosure tagging of AI-generated customer messages where required (CRC-AI-013).

## 9. Audit, Security & Compliance
- Proof-of-mailing evidence, consent decision logs, template approvals; PII minimized in provider payloads; provider DPAs (R01).

## 10. Success Metrics
| Metric | Type | Target (hypothesis) |
|---|---|---|
| Required notices evidenced within window | Lagging | 100% |
| Consent violations | Lagging | 0 |
| Duplicate sends | Leading | < 0.01% |
| Delivery rate (valid addresses) | Leading | ≥ 98% |

## 11. Open Questions
- [Legal] [Blocking] Required-notice channel rules (electronic delivery consent, certified mail) per jurisdiction.
- [Engineering] [Non-blocking] Providers per channel and region.

## 12. Dependencies & Phasing
- Needs F06 templates/generation and R01 consent model; channel adapters (S02) in parallel. Required-notice path must be live before P02 cancellations and C04 denials go live.
