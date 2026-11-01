# PRD-F01 — Identity, Access & Multi-tenancy

| Field | Value |
|---|---|
| ID | PRD-F01 |
| Domain | Foundation |
| Tier | **DEEP** |
| Status | Draft v0.1 |
| Owner | Platform Identity (TBD) |
| Depends on | F02 (audit sink), F03 (event envelope) |
| Consumed by | Every PRD |
| Key contract rules | CRC-SEC-001…004, 011, 012; CRC-PER-003; CRC-AI-009; CRC-API-007 |

## 1. Problem Statement

The platform serves carriers, MGAs, producers, vendors, and policyholders at once, each of whom may see only a narrow slice of data. One cross-tenant or cross-party leak (a vendor seeing another claimant's medical records, a producer seeing a competitor's book) is a regulatory and reputational event. And because money moves on human (and AI-assisted) authority, the platform must know at every instant *who* may approve *what amount*, and prove it afterwards.

## 2. Goals

1. **Zero cross-tenant data exposure**, verified by automated isolation tests on every repository and by production canary probes.
2. **Every authorization decision is explainable**: for any access or denial, the platform can show which policy, attributes, and authority limit applied.
3. **Financial authority is enforced, not trusted**: 100% of reserve, payment, refund, write-off, and binding actions are checked against the actor's authority limit at execution time.
4. **AI Agents never exceed human permissions**: effective AI permissions are always a subset of the invoking principal's.
5. **Onboarding a new organization** (agency, vendor firm) with correct scoping takes under 1 business day without engineering involvement.

## 3. Non-Goals

- **Building an identity provider.** Authentication is delegated to external OIDC IdPs; the platform federates.
- **Customer identity proofing (KYC) beyond account linking.** Handled by R03/S02 adapters where required.
- **HR-grade workforce management** (org charts, payroll). Only attributes needed for authorization are mastered here.
- **Cross-tenant data sharing** (e.g., reinsurer access to cedent data). P2; tenancy model must not preclude it.

## 4. Personas & User Stories

**Tenant security administrator**
- As a tenant security admin, I want to define roles from permission bundles so that I can grant least privilege without engineering changes.
- As a tenant security admin, I want quarterly access-review exports per role and organization so that I can certify access for auditors.
- As a tenant security admin, I want break-glass access that requires a reason and expires automatically so that emergencies don't create standing privilege.

**Claims manager**
- As a claims manager, I want to assign reserve and payment authority limits by role, LOB, and individual so that adjusters can act within their experience level.
- As a claims manager, I want payments above an adjuster's authority to route automatically to someone with enough authority so that nothing is approved by the wrong person.

**Producer agency principal**
- As an agency principal, I want to manage my agency's users and their book-of-business visibility so that my producers see only their clients.

**Vendor user**
- As a body-shop estimator, I want to see only the claims assigned to my shop, only while the assignment is active, so that I can work without seeing unrelated data.

**Policyholder**
- As a policyholder, I want to log in passwordlessly and see only my own policies and claims.

**Edge cases**
- As a security admin, I want a terminated employee's sessions revoked within 5 minutes of IdP deprovisioning.
- As an adjuster, when my authority is insufficient, I want a clear message showing my limit and who can approve, not a generic 403.

## 5. Domain Model & Invariants

| Entity | Key attributes |
|---|---|
| **Tenant** | id, legal name, data residency region, status, configuration bundle |
| **Organization** | id, tenant, type (`CARRIER_UNIT`, `MGA`, `PRODUCER_AGENCY`, `VENDOR_FIRM`, `TPA`, `REGULATOR_VIEW`), parent org, status |
| **Principal** | id, tenant, type (`HUMAN`, `SERVICE`, `EXTERNAL_CLIENT`, `AI_AGENT`), external subject (IdP), organization memberships, attributes (licenses, jurisdictions, LOBs, languages, skills), status |
| **Role** | id, tenant, name, permission set, scope template |
| **Permission** | action string `<ctx>:<resource>:<verb>` (e.g., `claims:payment:approve`), resource type |
| **Grant** | principal ↔ role, scope (organization / LOB / jurisdiction / resource set), validity period |
| **Authority Limit** | principal or role, action class (`RESERVE_CHANGE`, `PAYMENT_APPROVE`, `REFUND`, `WRITE_OFF`, `UW_DEVIATION`, `BIND`), LOB, currency, max amount (per transaction, per claim line cumulative), validity |
| **Relationship grant** | Time-bound, resource-specific access derived from business facts (vendor assigned to claim; producer of record on policy; insured on policy) |
| **Delegation** | delegator, delegate, actions, validity (e.g., AI Agent acting for adjuster; CSR acting for insured) |

**Invariants**
- INV-F01-1: Every principal belongs to exactly one tenant. No principal can hold grants in another tenant.
- INV-F01-2: An authorization decision is a pure function of (principal attributes, grants, relationship grants, resource attributes, action, environment, policy version); it is logged with the policy version.
- INV-F01-3: Relationship grants derive from business events and expire with them (vendor assignment closed ⇒ access revoked).
- INV-F01-4: AI Agent effective permissions = intersection(AI Agent grant, delegator's permissions, tenant policy) (CRC-AI-009).
- INV-F01-5: Authority limit changes are effective-dated and never retroactive; the limit used for a decision is recorded with the decision.
- INV-F01-6: Maker ≠ checker for actions configured as SoD-controlled.

## 6. Requirements

### P0 — Must-have

**F01-P0-01 Tenant isolation end-to-end.** Tenant is derived from the verified token, propagated via `TenantContext`, enforced by Postgres RLS (CRC-PER-003), carried in event envelopes, partitioned in search indexes and object storage prefixes.
*Acceptance:*
- Given a principal of tenant A, when any API, search, document, or export request targets tenant B data, then the response is 404 (no existence leak) and a security event is recorded.
- Every repository has an automated cross-tenant leakage test (CRC-TST-013).

**F01-P0-02 Federated authentication.** OIDC/SAML federation per tenant for workforce (SSO with MFA required for internal users), OIDC for producers and vendors, passwordless (email/SMS OTP or passkey) for customers. Session revocation honors IdP signals.
*Acceptance:* deprovisioned users lose access within 5 minutes; MFA is enforced for all internal roles with financial permissions.

**F01-P0-03 Policy Decision Point.** Central PDP evaluating RBAC + ABAC policies, exposed through `AuthorizationPort.decide(principal, action, resource, context)`. Policies are versioned and testable. Results include `allow/deny`, matched policy IDs, and obligations (e.g., field masks).
*Acceptance:* p99 decision latency < 10 ms in-region (cached policy bundle); every deny includes a machine-readable reason.

**F01-P0-04 Authority limits.** Configurable limits per role/principal × action class × LOB × currency. Commands carrying amounts check authority at execution; insufficient authority routes to an approval work item (C03) for an eligible approver.
*Acceptance:*
- Given an adjuster with payment authority 10,000 USD, when they request a 12,500 USD payment, then the payment order enters `PENDING_APPROVAL` and is routed to a principal whose authority ≥ 12,500.
- Cumulative limits per claim line are enforced (sum of the principal's approved payments on the line).

**F01-P0-05 Relationship-based access.** Producers see policies where they are producer of record (and their agency hierarchy per configuration); vendors see only assigned claims for the assignment's lifetime plus a configurable grace period; insureds/claimants see only their own records.
*Acceptance:* vendor access to a claim is revoked within 60 seconds of the assignment closing.

**F01-P0-06 Field-level obligations.** PDP returns masking obligations (e.g., SSN last-4 only, PHI hidden) honored by APIs and BFFs.

**F01-P0-07 Segregation of duties.** Configurable SoD rules (e.g., payee bank change and payment release by different principals; posting rule author ≠ publisher).
*Acceptance:* the same principal cannot both request and approve an SoD-controlled action, even with sufficient authority.

**F01-P0-08 Service and AI Agent principals.** Workload identity for services (mTLS + tokens). AI Agent principals are first-class with their own grants; runs execute with a delegated token binding AI Agent + invoking principal.

**F01-P0-09 Break-glass.** Time-boxed elevated access requiring reason and second-person approval (or post-hoc review if configured), auto-expiring, alerted, fully audited.

**F01-P0-10 Admin console & APIs.** Manage organizations, principals, roles, grants, authority limits, SoD rules; all changes audited (F02) and emitted as events.

### P1 — Fast follow
- **F01-P1-01** SCIM 2.0 provisioning from workforce IdPs.
- **F01-P1-02** Delegated administration for agency principals and vendor firm admins within scoped templates.
- **F01-P1-03** Step-up authentication for sensitive operations (payee changes, large payments, PII export).
- **F01-P1-04** Access recertification campaigns with reminders and auto-revoke of uncertified grants.
- **F01-P1-05** Producer license and appointment attributes synced from licensing sources (S02) and usable in policies (e.g., can bind only in licensed jurisdictions).
- **F01-P1-06** Policy simulation: "what would this principal be able to do if granted role X?"

### P2 — Future
- **F01-P2-01** Cross-tenant sharing contracts (reinsurers, fronting carriers) with explicit data-sharing agreements.
- **F01-P2-02** Customer-delegated access (an insured authorizes a public adjuster or family member).

## 7. Interfaces & Events

**Ports / APIs**
- `AuthorizationPort.decide(...)` / batch `decideAll(...)`; `AuthorityPort.check(principal, actionClass, amount, lob, scope)`
- Admin REST: `/v1/organizations`, `/v1/principals`, `/v1/roles`, `/v1/grants`, `/v1/authority-limits`
- Token exchange for delegation (AI Agent on-behalf-of)

**Events published**: `identity.principal.created.v1`, `identity.principal.deactivated.v1`, `identity.grant.assigned.v1`, `identity.grant.revoked.v1`, `identity.authority-limit.changed.v1`, `identity.break-glass.activated.v1`

**Events consumed**: `vendors.assignment.accepted.v1` / `…closed.v1` (relationship grants), `policy.term.bound.v1` (producer-of-record & insured relationships), `claims.party.added.v1` (claimant relationships)

## 8. AI Integration Points
- AI Agents are principals; no AI Agent has grants for denylisted actions (CRC-AI-002).
- P1: anomaly summarization of access-review data for security admins (internal L2 assist).

## 9. Audit, Security & Compliance
- All grant, limit, role, and SoD changes produce audit records with before/after.
- Authorization decisions on sensitive resources and all denials are logged as security events (sampled allows for non-sensitive reads).
- Supports SOC 2 access-control evidence and regulator requests for "who could access this claim on date X" (reconstructable from effective-dated grants).

## 10. Success Metrics

| Metric | Type | Target (hypothesis) | Measurement |
|---|---|---|---|
| Cross-tenant exposure incidents | Lagging | 0 | Security incident register, canary probes |
| PDP p99 latency | Leading | < 10 ms | OTel metrics |
| Authority checks on financial commands | Leading | 100% | Audit records with authority result ÷ financial commands |
| Deprovision-to-revocation time | Leading | p95 < 5 min | IdP event → session revoked |
| Access review completion | Lagging | ≥ 98% of grants certified per quarter | Recertification reports |

## 11. Open Questions
- [Engineering] [Blocking] PDP engine: OPA (Rego) vs. Cedar vs. embedded — decide by ADR considering policy testability and latency.
- [Security] [Blocking] Customer authentication: passkeys-first vs. OTP-first per tenant market.
- [Legal] [Non-blocking] Data residency: which tenants require in-region-only processing, including for AI providers?
- [Product] [Non-blocking] Should producer hierarchy visibility default to "agency sees all" or "producer sees own"?

## 12. Dependencies & Phasing
- **First**: tenant context, RLS pattern, PDP port with static policies, service identity — every other context depends on these.
- **Then**: authority limits and SoD (required before C05/F04 payments go live), relationship grants (required before X01/X02/V01 external users).
- **Parallel**: admin console UI, SCIM, recertification.
