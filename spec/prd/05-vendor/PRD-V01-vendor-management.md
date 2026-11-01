# PRD-V01 — Vendor & Third-Party Management

| Field | Value |
|---|---|
| ID | PRD-V01 |
| Domain | Vendor & Third-Party Management |
| Tier | **PARALLEL** |
| Status | Draft v0.1 |
| Owner | Claims Vendor Services (TBD) |
| Depends on | C02 (claims, parties), C03 (investigation plans, work items), C05 (expense payments, salvage), F01 (vendor orgs, relationship grants), F04 (payee verification, payment orders), F06 (estimates, invoices, photos), F05 (selection rules, rate validation), R03 (sanctions/fraud screening), S02 (repair networks, estimating platforms, bill review) |
| Consumed by | C03, C05, A01 |
| Key contract rules | CRC-SEC-004; CRC-IDEM-*; CRC-CFG-002; CRC-PER-009 |

## 1. Problem Statement

A large share of claim cost flows through vendors: repair shops, independent adjusters, appraisers, investigators, medical providers, rental, towing, salvage, restoration, and defense counsel. Without structured assignment, SLAs, rate agreements, and invoice validation, carriers overpay, cycle times stretch, and vendor performance is anecdotal. Vendors also need secure, scoped access to exactly the claims they work. The platform needs a vendor network registry, rule-based assignment respecting customer choice rules, SLA tracking, performance scoring, a vendor portal, and invoice validation feeding claim expense payments.

## 2. Goals

1. **Faster cycle times**: assignment-to-inspection p80 within SLA for ≥ 90% of assignments.
2. **Cost control**: invoice validation catches ≥ 95% of seeded rate/duplicate errors; vendor-related leakage baseline then −20%.
3. **Secure access**: 100% of vendor access scoped to active assignments (F01 relationship grants).
4. **Performance transparency**: scorecards for 100% of active vendors refreshed weekly.
5. **Customer choice compliance**: 0 violations of anti-steering/right-to-choose rules.

## 3. Non-Goals
- **Building estimating software** (integrate estimating platforms via S02).
- **Medical bill review engine** (integrate; V01 orchestrates and records results).
- **Vendor marketing/network recruitment CRM.**

## 4. Personas & User Stories
- As a **vendor manager**, I want to onboard vendors with licenses, insurance certificates, tax IDs, bank details, service areas, capacity, and rate agreements, with screening, so that only qualified vendors receive work.
- As an **adjuster**, I want the system to recommend the best vendor (proximity, capacity, performance, customer preference) and assign in one click from the investigation plan.
- As a **body shop**, I want to accept assignments, upload estimates/photos/supplements, update repair status, and submit invoices in a portal.
- As an **independent adjuster firm**, I want to receive field assignments, submit reports, and bill per our fee schedule.
- As a **claimant**, I want to choose my own repair shop and still have a smooth process.
- As **payment ops**, I want approved vendor invoices to become expense payments automatically on the right claim line and cost category.
- *Edge:* vendor declines or doesn't respond within SLA → auto-reassign to next eligible vendor.
- *Edge:* vendor bank details changed → SoD approval and verification before next payment.

## 5. Domain Model & Invariants
| Entity | Notes |
|---|---|
| **Vendor** | organization (F01 type `VENDOR_FIRM`), services (config codes), credentials (licenses, certificates with expiries), tax data, payee profiles (bank details encrypted, verification status), service areas, capacity, status (`ONBOARDING`, `ACTIVE`, `SUSPENDED`, `TERMINATED`) |
| **Rate Agreement** | service × region × effective dates → rates/fee schedules, caps, required documentation |
| **Vendor Assignment** | claim/claim line, service, vendor, SLA policy, instructions, state, milestones (contacted, inspected, estimate submitted, repair started/completed), outputs (documents) |
| **Vendor Invoice** | assignment, lines (service codes, qty, rate), totals, documents, validation results, state |
| **Scorecard** | metrics per vendor/service/region: cycle times, SLA adherence, severity vs. peers, re-inspection variance, CSAT, invoice exception rate |

**Assignment states**: `OFFERED → (ACCEPTED | DECLINED | EXPIRED) → IN_PROGRESS → (COMPLETED | CANCELLED)`; declines/expiry trigger reassignment.
**Invoice states**: `SUBMITTED → VALIDATING → (APPROVED | EXCEPTION → (APPROVED_WITH_ADJUSTMENT | REJECTED)) → PAYMENT_REQUESTED → PAID`.

**Invariants**
- INV-V01-1: Only `ACTIVE` vendors with valid credentials for the service and jurisdiction may receive offers.
- INV-V01-2: Vendor access to claim data exists only while an assignment is `OFFERED`/`ACCEPTED`/`IN_PROGRESS` (+ configured grace) and only for the assigned claim line data (minimum necessary).
- INV-V01-3: Invoice approval checks rate agreement, assignment scope, duplicates (same vendor + invoice number, or same service dates/amount), and estimate alignment; exceptions need adjuster/vendor-manager decision with reason.
- INV-V01-4: Selection rules honor customer choice and jurisdictional anti-steering rules (F05); a customer-chosen out-of-network vendor is supported via a non-network assignment path.

## 6. Requirements
### P0
- **V01-P0-01** Vendor registry & onboarding with credential tracking, expiry alerts, sanctions screening (R03), payee verification (F04).
- **V01-P0-02** Rate agreements as effective-dated configuration.
- **V01-P0-03** Selection & assignment rules (proximity, capacity, performance, service, customer choice) with explanation; manual override with reason.
- **V01-P0-04** Assignment lifecycle with SLA timers and auto-reassignment.
- **V01-P0-05** Vendor portal (bff-vendor): assignments, claim-scoped data, uploads (F06), status milestones, invoices, messages; vendor user management by firm admin (F01 delegated admin).
- **V01-P0-06** Invoice validation and approval → C05 expense payment request.
  *Acceptance:* Given an invoice line priced above the rate agreement, validation flags it with the expected rate and the adjuster can approve-with-adjustment; the payment equals the adjusted amount.
- **V01-P0-07** Scorecards and performance metrics.
- **V01-P0-08** Integration adapters for at least one repair network/estimating platform and one IA network (S02).
### P1
- **V01-P1-01** Medical bill review integration and fee-schedule application results.
- **V01-P1-02** Legal bill review (task/activity codes, guidelines compliance) for defense counsel.
- **V01-P1-03** Rental management (direct billing, day limits per coverage).
- **V01-P1-04** Re-inspection sampling and variance tracking.
### P2
- **V01-P2-01** Dynamic capacity marketplace for CAT surges.
- **V01-P2-02** Virtual-card vendor payments.

## 7. Interfaces & Events
- APIs: `/v1/vendors`, `/v1/vendors/{id}/rate-agreements`, `/v1/vendor-assignments`, `…/accept|decline|milestones|complete|cancel`, `/v1/vendor-invoices`, `…/validate|approve|reject`.
- **Published**: `vendors.vendor.activated.v1`, `…suspended.v1`, `vendors.assignment.offered.v1`, `…accepted.v1`, `…declined.v1`, `…completed.v1`, `…closed.v1`, `vendors.invoice.submitted.v1`, `…approved.v1`, `…rejected.v1`, `vendors.credential.expiring.v1`.
- **Consumed**: `claims.work-item.created.v1` (investigation activities), `claims.claim.closed.v1`, `ledger.payment-order.settled.v1`, `fraud.screening.completed.v1`.

## 8. AI Integration Points
- AI02 extracts estimate/invoice line items for validation (L3 proposals).
- AI06 drafts vendor instructions and follow-ups (L3).
- P1: estimate anomaly explanation (L2) comparing to similar repairs.

## 9. Audit, Security & Compliance
- Credential checks, assignments, overrides, invoice decisions audited.
- Vendor access minimum-necessary; PHI only to medical vendors with appropriate agreements.
- Anti-steering compliance evidence per assignment.

## 10. Success Metrics
| Metric | Type | Target (hypothesis) |
|---|---|---|
| Assignments meeting inspection SLA | Leading | ≥ 90% |
| Invoice validation catch rate (seeded) | Leading | ≥ 95% |
| Vendor access outside active assignment | Lagging | 0 |
| Vendor-related leakage | Lagging | −20% vs. baseline |
| Repair cycle time (auto physical damage) | Lagging | Baseline, then −15% |

## 11. Open Questions
- [Claims] [Blocking] Launch vendor categories and networks/integrations.
- [Legal] [Blocking] Anti-steering and right-to-choose requirements per jurisdiction.
- [Product] [Non-blocking] Vendor portal vs. integration-only for large networks.

## 12. Dependencies & Phasing
- Needs C02/C03 assignment hooks, F01 vendor org model & relationship grants, F06 uploads, C05 expense payment API. Portal and scorecards can be developed in parallel.
