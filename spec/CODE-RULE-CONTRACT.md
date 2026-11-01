# Code Rule Contract

| | |
|---|---|
| Version | 1.1.0 |
| Status | **Normative** — applies to every module in this repository |
| Date | 2026-11-01 |
| Owners | Architecture group + DEEP-domain CODEOWNERS |

This contract is the agreement between product intent (the PRDs) and the code. It turns the platform's non-negotiables (auditability, financial integrity, deterministic coverage, governed AI, tenant isolation) into checkable rules.

---

## 0. How this contract works

- **Keywords.** MUST, MUST NOT, SHOULD, SHOULD NOT, MAY are used as defined in RFC 2119.
- **Rule IDs.** Every rule has a stable ID `CRC-<AREA>-<NNN>`. IDs are never reused; retired rules stay listed as *Retired*.
- **Enforcement tags.**
  - **[Auto]** — CI-blocking automated check (ArchUnit, Error Prone, lint, test harness).
  - **[Runtime]** — enforced by the platform at runtime (DB grants, PDP, harness).
  - **[Review]** — enforced through the PR checklist; reviewers cite the rule ID.
- **Exceptions.** Only through an ADR in `docs/adr/` naming the rule ID, scope, rationale, and expiry date. Code-level suppression MUST cite it: `@ContractException(rule = "CRC-DOM-004", adr = "ADR-0042")`. Suppressions without a valid, unexpired ADR fail the build. **[Auto]**
- **Changes.** A PR to this file needs two approvals from the architecture group, a semantic version bump, and a changelog entry (Appendix C). New rules start as SHOULD for one minor version before becoming MUST unless they close a security or financial-integrity gap.
- **Precedence.** Contract > module README > code comments. When a PRD conflicts with this contract, the contract wins until the conflict is resolved by ADR.

---

## 1. Technology baseline

Baseline choices. Changing any row requires an ADR.

| Concern | Baseline |
|---|---|
| Language | Java 21 LTS minimum (records, sealed interfaces, pattern matching, virtual threads); new modules MAY target the newest LTS once the build image supports it |
| Topology | One independently deployable microservice per bounded context, plus BFFs ([ADR-0001](../docs/adr/0001-microservices-per-bounded-context.md)) |
| Application framework | Spring Boot (current GA line), used only in `-adapters` and the deployable `-service` module (CRC-ARCH-013) |
| Build | Gradle wrapper (Kotlin DSL), version catalog `gradle/libs.versions.toml`, convention plugins in `build-logic/`, one multi-module build |
| Database | PostgreSQL 16+; one schema per bounded context |
| Migrations | Flyway |
| Messaging | Apache Kafka (or wire-compatible), Schema Registry, Avro schemas |
| Long-running orchestration | Domain-owned process managers; a durable workflow engine (e.g., Temporal) MAY be used for timers/sagas but never owns domain state (CRC-SM-008) |
| Authorization | External PDP behind `AuthorizationPort` (OPA or Cedar — decided by ADR in F01) |
| Observability | OpenTelemetry (traces, metrics, logs), Micrometer, structured JSON logging via SLF4J |
| Testing | JUnit Jupiter (version managed by the Spring Boot BOM), AssertJ, jqwik (property-based), Testcontainers, ArchUnit, PIT, Pact |
| Static analysis | Spotless (google-java-format), Error Prone + NullAway, SpotBugs, dependency/CVE scanning, secret scanning |
| API description | OpenAPI 3.1 (sync), AsyncAPI 3 (event catalog) |

---

## 2. Architecture & module boundaries (ARCH)

**CRC-ARCH-001 — Bounded contexts are the unit of modularity.** Each context (see `spec/README.md` §6) consists of Gradle modules:
`<ctx>-domain` (model, invariants), `<ctx>-application` (use cases, ports), `<ctx>-adapters` (persistence, messaging, HTTP, external), `<ctx>-api` (published contract: OpenAPI, event schemas, client DTOs), and `<ctx>-service` (the deployable Spring Boot application: wiring, configuration, bootstrap — no business logic). Modules live at `services/<ctx>/<ctx>-<role>`. **[Auto: build-logic]**

**CRC-ARCH-002 — Dependency direction.** `service → adapters → application → domain`. `domain` depends only on the JDK and `shared-kernel`. `api` depends on nothing internal. **[Auto: build-logic + ArchUnit]**

**CRC-ARCH-003 — Pure domain.** `-domain` modules MUST NOT import Spring, Jakarta Persistence/Hibernate, Jackson, Kafka, HTTP clients, logging frameworks, or AI provider SDKs. **[Auto]**

**CRC-ARCH-004 — No reaching in.** A context MUST NOT read or write another context's tables or depend on another context's `-domain`, `-application`, or `-adapters` modules. Cross-context interaction is only via the other context's `-api` (sync) or its published events (async). **[Auto + Runtime: per-context DB role]**

**CRC-ARCH-005 — Schema ownership.** Each context owns exactly one DB schema and runs under a DB role granted only on that schema. **[Runtime]**

**CRC-ARCH-006 — Shared kernel is small.** `shared-kernel` holds only: typed-ID base types, `Money`, `CurrencyUnit`, `Percentage`, `Factor`, time types (`EffectivePeriod`, `BusinessDateTime`), `TenantId`, `PrincipalId`, `ActorRef`, `CorrelationId`, `IdempotencyKey`, `IdGenerator`, error base types, and the state-machine engine (`StateMachine`, `Guard`, `GuardResult`). Additions require an ADR. **[Review]**

**CRC-ARCH-007 — No co-location assumptions.** Contexts are deployed as separate services (ADR-0001). Code MUST NOT assume co-location: no shared transactions, in-memory calls, shared caches, or shared runtime state across contexts. Module rules stay valid if contexts are ever co-deployed again. **[Review]**

**CRC-ARCH-008 — Ports & adapters.** Every external dependency (DB, broker, PDP, model provider, payment rail, data provider, clock, ID generator) sits behind a port interface owned by the application layer. **[Auto]**

**CRC-ARCH-009 — No distributed transactions.** XA/2PC is prohibited. Multi-context workflows use sagas/process managers with explicit compensation, driven by events. **[Review]**

**CRC-ARCH-010 — Package naming.** `com.insurtech.<ctx>.<layer>.<subdomain>`; e.g., `com.insurtech.claims.domain.reserve`. **[Auto]**

**CRC-ARCH-011 — BFFs contain no domain logic.** Portal and mobile backends-for-frontends compose and adapt; they MUST NOT enforce business rules, compute money, or decide state. **[Review + Auto: BFF modules cannot depend on any `-domain`]**

**CRC-ARCH-012 — No cycles.** No dependency cycles between contexts, at module or event level (an event-level cycle must be justified in the context README). **[Auto for modules; Review for events]**

**CRC-ARCH-013 — Framework-free application layer.** `-application` modules MUST NOT depend on Spring or any web, persistence, messaging, or logging framework. Transactions go through `TransactionPort`; dependency wiring lives in `-service`. **[Auto: build-logic + ArchUnit]**

**CRC-ARCH-014 — Platform libraries carry no business logic.** `libs/platform-*` hold cross-cutting technical capabilities only (ports, envelopes, HTTP edge, contract tests). Libraries may depend only on `shared-kernel` and lower platform libraries; new libraries require an ADR. **[Auto: build-logic + Review]**

**CRC-ARCH-015 — Independently deployable services.** Each `-service` and BFF has its own configuration, port, DB role and schema, health/readiness probes, and SLOs (DEEP). It starts, runs, and degrades without any other service being co-deployed; missing dependencies surface as retries, circuit-breaker states, or work items (CRC-ERR-004). **[Runtime + Review]**

---

## 3. Domain modeling (DOM)

**CRC-DOM-001 — Typed identifiers.** Every domain identifier is a dedicated type (`record ClaimId(UUID value)`). Raw `String`, `UUID`, or `long` IDs MUST NOT appear in public signatures of `-domain` or `-application`. **[Auto: ArchUnit signature rule]**

**CRC-DOM-002 — Value objects validate on construction.** Domain concepts (`Money`, `Percentage`, `Limit`, `Deductible`, `PolicyNumber`, `ClaimNumber`, `Vin`, `PostalAddress`, `EmailAddress`, `CoverageCode`) are immutable records whose canonical constructor rejects invalid state. An invalid value object cannot exist. **[Review]**

**CRC-DOM-003 — Intention-revealing behavior, no setters.** Aggregates change only via business methods (`claimLine.close(reason, actor)`). Public setters are prohibited in `-domain`. **[Auto]**

**CRC-DOM-004 — One aggregate per transaction.** A transaction modifies at most one aggregate instance. Other aggregates are referenced by ID only. **[Review + Auto: repository usage check in handlers]**

**CRC-DOM-005 — Closed vs. open sets.** Closed, platform-defined sets (lifecycle states, actor types, debit/credit) are Java enums or sealed types. Open, business-configurable sets (coverage codes, causes of loss, reason codes, cost categories, segments, document types, jurisdictions) are config-backed value types, **never Java enums**, so a new product does not require a code change. **[Review]**

**CRC-DOM-006 — No nulls.** Domain and application code MUST NOT accept or return `null`. `Optional` is used only as a query return type. **[Auto: NullAway strict]**

**CRC-DOM-007 — Immutability.** Value objects are records; collections returned from aggregates are unmodifiable. **[Auto: Error Prone]**

**CRC-DOM-008 — No boolean state.** Multi-state concepts MUST NOT be modeled as booleans or nullable timestamps (`isClosed`, `cancelledAt == null`). **[Review]**

**CRC-DOM-009 — Typed domain errors.** Business rule violations throw subclasses of `DomainException` carrying a stable error code (e.g., `CLAIM_LINE_HAS_OUTSTANDING_RESERVE`). Generic `RuntimeException`/`IllegalArgumentException` MUST NOT signal business rule violations. **[Auto]**

**CRC-DOM-010 — Deterministic domain.** Domain code performs no I/O and never reads the clock, randomness, or environment directly. Time and IDs come through injected `Clock` / `IdGenerator` ports. **[Auto]**

**CRC-DOM-011 — Ubiquitous language.** Names follow `spec/GLOSSARY.md`. Insurance agents/brokers are `Producer`; LLM agents are `AiAgent`. A type named `Agent` is prohibited. **[Auto: name lint + Review]**

**CRC-DOM-012 — No LOB subclassing.** Line-of-business differences are expressed through product configuration and risk-object schemas (P01), not subclasses (`AutoClaim extends Claim` is prohibited). **[Review]**

---

## 4. State machines (SM)

Applies to every lifecycle aggregate, including: Quote, Policy Term, Policy Transaction, Loss Report, Claim, Claim Line, Work Item, Recovery, Payment Order, Invoice, Billing Account delinquency, Vendor Assignment, Rule Set, Product Version, AI Proposal, Data Subject Request.

**CRC-SM-001 — Explicit transition table.** Each lifecycle aggregate declares its state machine as a single, declarative transition table in code: `(fromState, command) → guards → toState, events`. Transition logic MUST NOT be scattered across services. **[Review + Auto: one `StateMachine` definition per aggregate]**

**CRC-SM-002 — Exhaustive states.** States are an enum or sealed type; `switch` over state is exhaustive with no `default` branch. **[Auto: Error Prone]**

**CRC-SM-003 — Pure guards with reasons.** Guards are side-effect-free functions returning `GuardResult` (pass, or fail with error codes). All failing guards are reported, not just the first. **[Review]**

**CRC-SM-004 — Illegal transitions fail loudly.** An illegal `(state, command)` pair throws `IllegalStateTransitionException(aggregate, from, command)`. It is never ignored or silently no-op'd — except an idempotent replay of the same command (CRC-IDEM-002). **[Auto: test]**

**CRC-SM-005 — Machine-readable and fully tested.** The transition table is exported at build time (Mermaid + JSON) to `docs/state-machines/`. Tests MUST cover 100% of legal transitions and assert rejection of every illegal `(state, command)` pair. **[Auto]**

**CRC-SM-006 — Atomic transition.** New state, domain event(s) (via outbox), and audit record are persisted in the same database transaction. **[Auto: handler test harness]**

**CRC-SM-007 — No out-of-band state writes.** State columns are written only by the aggregate's repository. Data fixes use a *corrective command* (`CorrectClaimStateCommand`) that is authorized, audited, and references a ticket. Raw `UPDATE … SET status` in migrations or scripts is prohibited. **[Auto: SQL lint + Review]**

**CRC-SM-008 — Timers are commands.** SLA deadlines, expirations, earning runs, and delinquency steps are scheduled commands against the aggregate, subject to the same guards. Cron jobs MUST NOT mutate state directly. **[Review]**

**CRC-SM-009 — Optimistic concurrency.** Aggregates carry a `version`. Commands carry the expected version (HTTP `If-Match`); mismatches return `409`. **[Auto]**

**CRC-SM-010 — Orthogonal regions stay orthogonal.** Independent dimensions (e.g., Claim Line lifecycle vs. coverage position vs. litigation flag) are modeled as separate state fields with separate tables; cross-dimension constraints are expressed as guards. **[Review]**

---

## 5. Money & numerics (MON)

**CRC-MON-001 — `Money` only.** Monetary amounts use `Money` (`BigDecimal` amount + `CurrencyUnit`). `double`/`float` are prohibited for money, rates, factors, and percentages anywhere in financial computation. **[Auto: Error Prone + ArchUnit]**

**CRC-MON-002 — Currency-safe arithmetic.** Arithmetic goes through `Money` operations; mixing currencies throws `CurrencyMismatchException`. **[Auto: test]**

**CRC-MON-003 — Explicit rounding.** Every rounding names its `RoundingMode` and scale. Default: `HALF_EVEN` at the currency's minor-unit scale. Rating intermediates use the precision declared in the rating plan and round only at points recorded in the rating worksheet. **[Auto: no `BigDecimal` division without scale + mode]**

**CRC-MON-004 — Exact allocation.** Splitting an amount (installments, pro-rata, multi-payee, commission splits) uses largest-remainder allocation so parts sum exactly to the whole. **[Auto: property test]**

**CRC-MON-005 — Direction, not sign, in the ledger.** Ledger amounts are non-negative with an explicit `DEBIT`/`CREDIT` side. Sign-based semantics are prohibited in ledger APIs. **[Auto]**

**CRC-MON-006 — Typed ratios.** Percentages and factors are `Percentage` / `Factor`, never raw `BigDecimal` in signatures. **[Auto]**

**CRC-MON-007 — FX provenance.** Every conversion records rate, source, and rate timestamp; converted amounts keep both transaction and functional currency. **[Review]**

**CRC-MON-008 — Wire format.** In JSON, money is `{"amount": "123.45", "currency": "USD"}` with the amount as a string. JSON numbers for money are prohibited. **[Auto: API lint]**

---

## 6. Time (TIME)

**CRC-TIME-001 — Right type for the job.** `Instant` for system/transaction time; `LocalDate` / `ZonedDateTime` (via `BusinessDateTime`) for business effective time. `java.util.Date`, `Calendar`, and `java.sql.Timestamp` are prohibited outside adapters. **[Auto]**

**CRC-TIME-002 — Injected clock.** `Instant.now()`, `LocalDate.now()`, `System.currentTimeMillis()` without an injected `Clock` are prohibited. **[Auto]**

**CRC-TIME-003 — Bitemporal regulated data.** Policy versions, coverage snapshots, product configuration, rule sets, rating plans, party data relevant to decisions, and reserves record valid time (`effectiveFrom`, `effectiveTo`) and transaction time (`recordedAt`). Queries that drive decisions take both `asOf` (effective) and `knownAt` (transaction) parameters. **[Review]**

**CRC-TIME-004 — Half-open intervals.** Periods are `[start, end)`. Policy term boundaries carry an explicit time and zone as defined by product configuration (e.g., 12:01 a.m. local standard time at the insured location). **[Auto: `EffectivePeriod` type]**

**CRC-TIME-005 — Storage in UTC.** Timestamps are stored as `timestamptz` (UTC); zone rules are configuration. **[Auto: migration lint]**

**CRC-TIME-006 — Precision is data.** A loss date/time records its precision (`DATE_ONLY`, `APPROXIMATE`, `EXACT`) and zone. Coverage evaluation MUST handle each precision explicitly. **[Review]**

**CRC-TIME-007 — No implicit "now" in decisions.** Rule, rating, and coverage evaluation MUST receive the effective date as an explicit input; they never default to the current date. **[Auto: Rules port signature]**

---

## 7. Persistence (PER)

**CRC-PER-001 — Postgres, one schema per context.** (See CRC-ARCH-005.)

**CRC-PER-002 — Forward-only migrations.** Flyway migrations are immutable once merged. Breaking schema changes use expand → migrate → contract across releases; every migration is backward compatible with the previous application version. **[Auto: migration checksum + compatibility test]**

**CRC-PER-003 — Tenant column + RLS.** Every table with tenant data has `tenant_id NOT NULL`, is covered by Postgres Row-Level Security, and the application sets the tenant per transaction from `TenantContext`. A query without tenant context fails. **[Auto: migration lint + cross-tenant leakage test per repository]**

**CRC-PER-004 — Version column.** Aggregate root tables carry `version bigint NOT NULL` for optimistic locking. **[Auto]**

**CRC-PER-005 — Row metadata ≠ audit.** `created_at/by`, `updated_at/by` columns are required on aggregate tables but never substitute for the audit trail (§12). **[Review]**

**CRC-PER-006 — No hard deletes of regulated data.** `DELETE` is prohibited on policy, claim, ledger, audit, decision, document-metadata, and communication tables. Removal happens only via the retention/erasure service (R01). **[Runtime: grants + Auto: SQL lint]**

**CRC-PER-007 — Append-only stores.** Ledger, audit, decision, and event-archive tables deny `UPDATE` and `DELETE` to application roles. **[Runtime]**

**CRC-PER-008 — Persistence models stay in adapters.** JPA entities or jOOQ records live in `-adapters` and are mapped to domain objects. Domain objects MUST NOT carry persistence annotations. **[Auto]**

**CRC-PER-009 — Field-level encryption for sensitive PII.** Fields classified `SENSITIVE_PII`, `PHI`, or `FINANCIAL_ACCOUNT` (R01) are encrypted at application level with envelope encryption (KMS) and per-data-subject keys to support crypto-shredding. Lookups use blind indexes or tokens. **[Auto: classification annotation check]**

**CRC-PER-010 — Query hygiene.** No `SELECT *` in production code; large result sets use keyset pagination; every query on a hot path has an index review. **[Review]**

**CRC-PER-011 — Read models are disposable.** Projections and read models MUST be rebuildable from events or source tables; they are never the system of record. **[Review]**

---

## 8. Events (EVT)

**CRC-EVT-001 — Transactional outbox.** Every state transition of a lifecycle aggregate emits a domain event. Integration events are written to the outbox in the same transaction as the state change; a relay publishes them. Publishing directly to the broker from business code is prohibited. **[Auto]**

**CRC-EVT-002 — Naming.** `<context>.<aggregate>.<past-tense-verb>.v<N>`, kebab-case segments, e.g., `claims.claim-line.closed.v1`. Events state facts; commands are never sent on event topics. **[Auto: schema lint]**

**CRC-EVT-003 — Mandatory envelope.** `eventId` (ULID), `eventType`, `schemaVersion`, `tenantId`, `aggregateType`, `aggregateId`, `aggregateVersion`, `occurredAt` (Instant), `effectiveAt` (business time, when applicable), `correlationId`, `causationId`, `actor` (`ActorRef` incl. type and on-behalf-of), `traceparent`, `payload`. **[Auto]**

**CRC-EVT-004 — Compatibility.** Schemas are registered with `BACKWARD_TRANSITIVE` compatibility enforced in CI. Breaking changes ship as a new major version, dual-published for a declared migration window. **[Auto]**

**CRC-EVT-005 — Idempotent consumers.** Consumers record processed `(consumerName, eventId)` in an inbox table in the same transaction as their side effects. **[Auto: consumer test harness]**

**CRC-EVT-006 — Ordering.** Partition key = `aggregateId`. Consumers use `aggregateVersion` to detect gaps and stale events and MUST tolerate reordering across aggregates. **[Review]**

**CRC-EVT-007 — Minimal PII.** Payloads carry IDs and business facts consumers need. Sensitive data is passed by reference, never by value, unless the PRD justifies it. **[Review + Auto: classification check on schemas]**

**CRC-EVT-008 — Immutable facts.** Published events are never edited or deleted; corrections are new compensating events (`…corrected.v1`, `…reversed.v1`). **[Runtime]**

**CRC-EVT-009 — DLQ and replay.** Every consumer has a DLQ, poison-message handling, and replay tooling. DLQ depth > 0 in a DEEP context pages the on-call. **[Runtime]**

**CRC-EVT-010 — Catalog.** The AsyncAPI event catalog is generated from registered schemas and published with each release. **[Auto]**

---

## 9. Idempotency (IDEM)

**CRC-IDEM-001 — Required on mutation.** Every mutating HTTP endpoint requires an `Idempotency-Key` header; missing → `400`. **[Auto: API lint + runtime filter]**

**CRC-IDEM-002 — Semantics.** Keys are scoped by tenant + principal + operation. Same key + same request hash → original response replayed. Same key + different request → `422` with problem type `idempotency-key-reuse`. **[Auto: replay test per endpoint]**

**CRC-IDEM-003 — Financial keys are permanent.** Reserve changes, payment orders, refunds, write-offs, commission payouts, and ledger postings persist their idempotency key under a permanent unique constraint. General API keys are retained ≥ 24 hours. **[Runtime: unique constraint]**

**CRC-IDEM-004 — Derived keys for internal commands.** Commands issued by event consumers, schedulers, or AI tools derive deterministic keys from their cause (e.g., `sha256(causationEventId + commandType + target)`). **[Review]**

**CRC-IDEM-005 — Outbound money is idempotent.** Calls to payment rails pass a provider idempotency key; if the provider lacks one, the adapter performs status-query-before-retry. **[Review]**

**CRC-IDEM-006 — Retry discipline.** Only idempotent operations are retried, with exponential backoff and jitter, bounded attempts, then DLQ or work item. **[Review]**

---

## 10. APIs (API)

**CRC-API-001 — Contract-first.** OpenAPI 3.1 specs live in `<ctx>-api`, are linted (Spectral ruleset in repo), and generate server interfaces and clients. **[Auto]**

**CRC-API-002 — Versioning.** URI major version (`/v1/`). Breaking changes need a new major. Deprecations carry `Deprecation` and `Sunset` headers and a minimum 6-month window for external consumers. **[Auto: breaking-change diff]**

**CRC-API-003 — Problem Details.** Errors use RFC 9457 Problem Details with stable `type` URIs and the domain error code. **[Auto]**

**CRC-API-004 — Commands are intentions.** State changes are explicit command resources: `POST /v1/claims/{claimId}/close`, `POST /v1/policy-terms/{termId}/cancellations`. Generic `PATCH {status: …}` is prohibited for lifecycle state. **[Auto: lint]**

**CRC-API-005 — DTOs only.** Persistence entities and domain objects are never serialized directly. **[Auto]**

**CRC-API-006 — Cursor pagination.** List endpoints use opaque cursors with a capped page size. **[Auto]**

**CRC-API-007 — Context propagation.** Requests propagate `traceparent` and `X-Correlation-Id`. Tenant identity is derived from the verified token, never trusted from the path or body alone. **[Runtime]**

**CRC-API-008 — No PII in URLs.** PII MUST NOT appear in paths or query strings (use POST search with body). **[Auto: lint]**

**CRC-API-009 — Formats.** Money per CRC-MON-008; dates ISO-8601; enums as UPPER_SNAKE strings; IDs as opaque strings. **[Auto]**

---

## 11. Security & access (SEC)

**CRC-SEC-001 — Deny by default; authorize first.** Every application-service command and query calls `AuthorizationPort` (directly or via `@Authorized`) before loading or mutating data. Unannotated public application-service methods fail the build. **[Auto: ArchUnit]**

**CRC-SEC-002 — Tenant context is mandatory.** Established at the edge from a verified token, carried in-process via `TenantContext`, and across async boundaries via the event envelope. **[Runtime]**

**CRC-SEC-003 — Authority limits on money and decisions.** Reserve changes, payments, refunds, write-offs, UW pricing deviations, and binding check the acting principal's authority limit in the application layer, and the check is recorded in the audit record. **[Auto: handler test]**

**CRC-SEC-004 — Segregation of duties.** Configured actions (e.g., payments over threshold, payee bank-detail changes, posting-rule publication) require maker ≠ checker. **[Runtime]**

**CRC-SEC-005 — Secrets.** No secrets in code or config repos; use the secret manager. Secret scanning runs on every PR. **[Auto]**

**CRC-SEC-006 — No PII in telemetry.** PII never appears in logs, metric labels, trace attributes, exception messages, or non-essential event payloads. Logging uses the masking encoder; tests assert masking. **[Auto]**

**CRC-SEC-007 — Validate twice.** Schema validation at the edge, invariant validation in value objects. **[Review]**

**CRC-SEC-008 — Dependency hygiene.** Critical/high CVEs without an approved exception block merge. **[Auto]**

**CRC-SEC-009 — Verification standard.** External-facing surfaces meet OWASP ASVS Level 2; authentication, payment, and payee-management flows meet Level 3. **[Review + pen test]**

**CRC-SEC-010 — No card data.** Primary account numbers never enter platform systems; card and bank capture uses PSP tokenization. **[Review]**

**CRC-SEC-011 — Workload identity.** Service-to-service calls use mTLS plus workload identity tokens. **[Runtime]**

**CRC-SEC-012 — Read-audit for sensitive data.** Viewing `SENSITIVE_PII`, `PHI`, or `FINANCIAL_ACCOUNT` fields writes a read-audit record. **[Runtime]**

---

## 12. Audit & decision trace (AUD)

**CRC-AUD-001 — No state change without audit.** Every state-changing command writes an `AuditRecord` through `AuditPort` in the same transaction (outbox-delivered). **[Auto: command handler harness]**

**CRC-AUD-002 — Audit record content.** Actor (`ActorRef`: type, ID, on-behalf-of chain), command type, target aggregate type/ID/version before → after, change set, reason code (+ free text where required), authority check result, correlation/causation IDs, channel, recorded-at, effective-at. **[Auto: schema]**

**CRC-AUD-003 — Decision records.** Every business decision — rule evaluation, AI output, or human determination under authority — writes a `DecisionRecord` with input references, rule/model/prompt versions, outputs, explanation, confidence (if any), and the final action. **[Auto: Rules port and harness emit automatically; Review for human decisions]**

**CRC-AUD-004 — Overrides need reasons.** A human outcome that differs from a rule or AI recommendation records an override with a reason code and justification text. **[Auto]**

**CRC-AUD-005 — Audit is not logging.** Logs are operational and short-lived; audit is regulated and durable. Code MUST NOT rely on logs for audit purposes. **[Review]**

**CRC-AUD-006 — Cannot be disabled.** No configuration, flag, or code path may suppress audit or decision records. **[Review]**

---

## 13. Configuration, rules & products (CFG)

**CRC-CFG-001 — Business variability is configuration.** Coverages, limits, deductibles, eligibility, rating factors, thresholds, reason codes, SLAs, jurisdictional rules, communication templates, and posting rules live in versioned, effective-dated configuration. Adding a product within supported patterns MUST NOT require a code change. **[Review]**

**CRC-CFG-002 — No hard-coded jurisdiction or LOB branches.** Conditionals on jurisdiction, LOB, product, or tenant codes (`if (state.equals("CA"))`) are prohibited in code. **[Auto: lint on known code literals + Review]**

**CRC-CFG-003 — Pure rules.** Rules are deterministic functions of explicit inputs, evaluated through `RulesPort`. Results carry the rule-set version, a content hash, and an explanation. **[Auto]**

**CRC-CFG-004 — Immutable published versions.** Published configuration versions are immutable; changes create new versions resolved by caller-supplied effective date. **[Runtime]**

**CRC-CFG-005 — Configuration is reviewed like code.** Draft → review → approved → published, with four-eyes approval and automated tests (test cases bundled with the version). **[Runtime]**

**CRC-CFG-006 — Feature flags are for code rollout only.** Flags have an owner and an expiry and MUST NOT encode business or regulatory behavior. **[Review]**

---

## 14. AI Agents (AI)

**CRC-AI-001 — AI never mutates domain state directly.** AI Agents act only through registered tools. `MUTATE` tools are available to an AI Agent only at autonomy L4 and only for actions on the L4 allowlist. All other side effects go through `PROPOSE` tools that create Proposals for an approval gate. **[Runtime: harness]**

**CRC-AI-002 — Hard denylist.** Regardless of configuration, AI Agents MUST NOT: post ledger entries; create, approve, or release payments; set or change reserves; set coverage positions or deny claims; decline, cancel, or non-renew policies; make underwriting decisions; or send denial, ROR, adverse-action, or cancellation communications. This list is compiled into the harness, not configurable. **[Runtime + Auto: test]**

**CRC-AI-003 — One gateway.** All model calls go through the Model Gateway (`aiharness`). AI provider SDKs are banned outside the gateway module. **[Auto: ArchUnit]**

**CRC-AI-004 — Full run trace.** Every run records AI Agent ID/version, model ID/version, prompt template version, retrieved context references, tool calls with inputs/outputs, token counts, cost, latency, confidence, and outcome, linked to a DecisionRecord. **[Runtime]**

**CRC-AI-005 — Prompts are versioned artifacts.** Prompts live in the repository as templates (not inline string literals), are code-reviewed, and are bound to an eval suite. **[Auto: lint for inline prompts]**

**CRC-AI-006 — Eval-gated changes.** Any change to model, prompt, tool set, retrieval configuration, or thresholds must pass the AI Agent's eval suite thresholds in CI before promotion. **[Auto]**

**CRC-AI-007 — Structured outputs.** AI outputs consumed by code are validated against a JSON Schema. Invalid output → bounded retry → escalation to a human work item. **[Runtime]**

**CRC-AI-008 — Grounding.** Assertions about a specific policy, claim, party, or document must cite a source (document ID + span, or record ID + version). High-stakes AI Agents (AI03, AI05, AI07) block uncited assertions. **[Runtime]**

**CRC-AI-009 — Least privilege.** Effective permissions of an AI Agent run = intersection(AI Agent grant, invoking principal's permissions, tenant policy). **[Runtime: PDP]**

**CRC-AI-010 — Budgets.** Token, cost, time, and tool-call budgets are enforced per run and per tenant/day. Exceeding a budget stops the run and escalates. **[Runtime]**

**CRC-AI-011 — Data handling.** Data sent to model providers is minimized and redacted per the provider's data policy (R01). Only providers contracted for no training and approved retention are permitted; data residency is respected. **[Runtime + Review]**

**CRC-AI-012 — Rules win.** When deterministic rules and AI disagree, the rule outcome stands; the disagreement is recorded as a signal. **[Runtime]**

**CRC-AI-013 — Disclosure.** Customer-facing AI output is disclosed as AI-generated where required and never impersonates a human. **[Review]**

**CRC-AI-014 — Kill switch and fallback.** Every AI Agent has a per-tenant kill switch; every AI-assisted flow has a working manual path. **[Runtime + Auto: test with AI Agent disabled]**

**CRC-AI-015 — Fairness.** Protected attributes and proxies identified in fairness review MUST NOT be model inputs for underwriting, pricing, triage, or fraud models; eval suites include fairness tests. **[Auto: eval + Review]**

---

## 15. Errors & resilience (ERR)

**CRC-ERR-001 — Map at the edge.** Domain exceptions map to Problem Details in adapters; error codes are documented in `-api`. **[Auto]**

**CRC-ERR-002 — Never swallow.** Catching `Exception`/`Throwable` broadly, or empty catch blocks, are prohibited outside top-level adapters that log and rethrow/translate. **[Auto: Error Prone]**

**CRC-ERR-003 — Timeouts and breakers.** Every outbound call has a timeout; external adapters use circuit breakers and bulkheads. **[Review]**

**CRC-ERR-004 — Fail safe.** An external outage MUST NOT leave an aggregate in a partially applied state; work is retried from durable state or surfaced as a work item. **[Review]**

---

## 16. Observability (OBS)

**CRC-OBS-001 — End-to-end traces.** OpenTelemetry tracing across HTTP and async hops; the event envelope carries `traceparent`. **[Runtime]**

**CRC-OBS-002 — Structured logs.** JSON logs with `timestamp, level, service, traceId, spanId, tenantId, correlationId, eventName`; no PII (CRC-SEC-006); no `System.out`. **[Auto]**

**CRC-OBS-003 — Metrics.** Micrometer; RED metrics for every endpoint and consumer; business counters (`insurtech.<ctx>.<metric>`); bounded label cardinality — no entity IDs as labels. **[Auto: lint]**

**CRC-OBS-004 — AI telemetry.** AI spans include AI Agent, model, tokens, cost, latency, outcome, confidence, and the DecisionRecord ID. **[Runtime]**

**CRC-OBS-005 — SLOs.** Each DEEP service defines SLOs with burn-rate alerting before production. **[Review]**

**CRC-OBS-006 — Financial correlation.** Financial operation logs and metrics include the idempotency key or journal entry ID. **[Review]**

---

## 17. Testing (TST)

**CRC-TST-001 — Domain unit tests.** Domain logic is tested without mocking domain objects; only ports are faked. **[Review]**

**CRC-TST-002 — State machine coverage.** 100% legal-transition coverage and negative tests for every illegal pair (CRC-SM-005). **[Auto]**

**CRC-TST-003 — Property-based financial tests.** jqwik properties for: balanced journal entries, allocation sums, reversal nets to zero, reserve never negative, idempotent replay produces no new postings. **[Auto]**

**CRC-TST-004 — Coverage thresholds.** DEEP domain modules: ≥ 90% line coverage and ≥ 80% PIT mutation score. Other domain modules: ≥ 80% line coverage. Application modules: ≥ 80%. **[Auto]**

**CRC-TST-005 — Real infrastructure.** Integration tests use Testcontainers (PostgreSQL, Kafka). In-memory database substitutes are prohibited for persistence tests. **[Auto]**

**CRC-TST-006 — Contract tests.** Consumer-driven contracts (Pact) for sync APIs between contexts; schema compatibility checks for events. **[Auto]**

**CRC-TST-007 — Golden files.** Rating, coverage determination, and posting rules maintain golden-file regression suites per product/rule version; any diff fails the build unless the golden file is updated in the same PR with approval. **[Auto]**

**CRC-TST-008 — Architecture tests.** ArchUnit suites run in every module build. **[Auto]**

**CRC-TST-009 — AI evals.** Every AI Agent ships with an eval suite (F07) whose thresholds gate promotion. **[Auto]**

**CRC-TST-010 — Synthetic data.** Tests use synthetic data. Production data never enters non-production environments without approved irreversible masking. **[Runtime + Review]**

**CRC-TST-011 — Idempotency replay tests.** Every mutating endpoint and consumer has a replay test. **[Auto]**

**CRC-TST-012 — Time-travel tests.** Fixed-clock tests cover term boundaries, leap days, DST transitions, zone differences, back-dated and out-of-sequence transactions. **[Review]**

**CRC-TST-013 — Isolation tests.** Each repository has a cross-tenant leakage test. **[Auto]**

---

## 18. DEEP module gates

DEEP contexts: `identity`, `audit`, `eventing`, `ledger`, `rules`, `documents`, `aiharness`, `product`, `policy`, `claims`.

In addition to all rules above, changes to DEEP contexts require:

| Gate | Requirement |
|---|---|
| Review | Two approvals, at least one CODEOWNER of the context |
| Model changes | ADR for any change to aggregates, invariants, state machines, posting rules, or event schemas |
| Tests | CRC-TST-004 DEEP thresholds; property tests for invariants |
| Threat model | Updated for new external surfaces or permission changes |
| Performance | Load test for changes on hot paths (FNOL registration, coverage snapshot, posting, PDP) |
| Release | Progressive delivery (canary) with automated rollback on SLO breach |
| Runbook | Updated runbook for new failure modes |

---

## 19. Style & conventions (STY)

**CRC-STY-001 — Formatting.** Spotless with google-java-format; violations fail the build. **[Auto]**

**CRC-STY-002 — Static analysis.** Error Prone and NullAway (strict in `-domain` and `-application`), SpotBugs on all modules. **[Auto]**

**CRC-STY-003 — No Lombok.** Use records and explicit code. **[Auto]**

**CRC-STY-004 — Minimal visibility.** Package-private by default; `public` only for the module's intended surface. **[Review]**

**CRC-STY-005 — Naming patterns.** Commands `CloseClaimLineCommand`; events `ClaimLineClosed`; handlers `CloseClaimLineHandler`; ports `*Port`; adapters `*Adapter`; repositories `*Repository` (interfaces in domain/application); queries `*Query`; read models `*View`. **[Auto: naming lint]**

**CRC-STY-006 — No static mutable state or service locators.** Constructor injection only. **[Auto]**

**CRC-STY-007 — Comments explain why.** Public API types carry Javadoc explaining domain meaning and invariants. **[Review]**

**CRC-STY-008 — Complexity.** Methods with cyclomatic complexity > 15 need justification in review. **[Auto: report]**

---

## 20. Build, dependencies & delivery (DEL)

**CRC-DEL-001 — Pinned dependencies.** All versions in the version catalog; no dynamic or snapshot versions on the main branch. **[Auto]**

**CRC-DEL-002 — Reproducible builds and SBOM.** CycloneDX SBOM per deployable artifact. **[Auto]**

**CRC-DEL-003 — License allowlist.** New dependencies are reviewed; allowed: Apache-2.0, MIT, BSD-2/3, EPL-2.0, MPL-2.0 (file-level), others by ADR. AGPL/SSPL prohibited. **[Auto]**

**CRC-DEL-004 — Pipeline order.** format → compile (Error Prone/NullAway) → unit → ArchUnit → mutation (DEEP) → integration (Testcontainers) → contract → security (SCA, secrets, SAST) → AI evals (if `aiharness` assets changed) → package + SBOM. **[Auto]**

**CRC-DEL-005 — Trunk-based delivery.** Short-lived branches, Conventional Commits, PR template including the contract checklist (Appendix D). **[Auto: commit lint]**

**CRC-DEL-006 — Traceability.** Every PR references the PRD requirement IDs it implements (e.g., `C02-P0-04`) or a ticket. **[Auto: PR lint]**

**CRC-DEL-007 — Safe migrations in deploys.** Database migrations deploy before code that needs them and remain compatible with the previous code version (CRC-PER-002). **[Auto]**

---

## 21. Documentation (DOC)

**CRC-DOC-001 — ADRs.** Significant decisions are recorded as ADRs (`docs/adr/NNNN-title.md`: context, decision, consequences, status). **[Review]**

**CRC-DOC-002 — Context README.** Each context has a README: purpose, owned aggregates, invariants, API and event links, owning PRDs, runbook link. **[Auto: presence check]**

**CRC-DOC-003 — Generated diagrams.** State machine diagrams and event catalog are generated from code and schemas, not hand-drawn. **[Auto]**

---

## 22. Definition of Done

A change is done when:

- [ ] PRD requirement IDs referenced; acceptance criteria demonstrably met by tests
- [ ] All [Auto] rules green; no unapproved suppressions
- [ ] Authorization (CRC-SEC-001), audit (CRC-AUD-001), and idempotency (CRC-IDEM-001) present for every new command
- [ ] Events registered, compatible, and documented in the catalog
- [ ] State machine table and diagrams regenerated (if lifecycle touched)
- [ ] PII fields classified; masking and encryption verified
- [ ] Observability: traces, RED metrics, business counters, dashboards/alerts updated
- [ ] AI changes: eval suite passed, prompt/model versions bumped, kill switch verified
- [ ] Docs: context README / ADR / runbook updated as applicable

---

## Appendix A — Reference shapes (illustrative)

These illustrate the rules; they are not complete implementations.

**Typed ID and Money (CRC-DOM-001, CRC-MON-001/003)**

```java
public record ClaimId(UUID value) {
  public ClaimId { Objects.requireNonNull(value, "value"); }
}

public record Money(BigDecimal amount, CurrencyUnit currency) {
  public Money {
    Objects.requireNonNull(amount); Objects.requireNonNull(currency);
    amount = amount.setScale(currency.minorUnits(), RoundingMode.UNNECESSARY); // reject excess precision
  }
  public Money plus(Money other) { requireSameCurrency(other); return new Money(amount.add(other.amount), currency); }
  public boolean isNegative() { return amount.signum() < 0; }
  // allocate(List<Factor> weights) uses largest-remainder (CRC-MON-004)
}
```

**Explicit state machine (CRC-SM-001/002/004)**

```java
public enum ClaimLineState { OPEN, CLOSED, VOIDED }

static final StateMachine<ClaimLineState, ClaimLineCommand> MACHINE = StateMachine
    .<ClaimLineState, ClaimLineCommand>builder()
    .on(OPEN,   CloseClaimLine.class).guard(ClaimLineGuards::noOutstandingReserve)
                                     .guard(ClaimLineGuards::noOpenPaymentOrders)
                                     .guard(ClaimLineGuards::coveragePositionResolved)
                                     .to(CLOSED).emit(ClaimLineClosed::from)
    .on(CLOSED, ReopenClaimLine.class).guard(ClaimLineGuards::withinReopenWindow).to(OPEN).emit(ClaimLineReopened::from)
    .on(OPEN,   VoidClaimLine.class).guard(ClaimLineGuards::noFinancialPostings).to(VOIDED).emit(ClaimLineVoided::from)
    .build(); // any other (state, command) pair → IllegalStateTransitionException
```

**Command handler shape (CRC-SEC-001, CRC-SM-006, CRC-AUD-001, CRC-IDEM-002)**

```java
@Authorized(action = "claims:claim-line:close")
public CommandResult handle(CloseClaimLineCommand cmd) {
  return idempotency.execute(cmd.idempotencyKey(), cmd.requestHash(), () ->
      tx.inTransaction(() -> {
        var line = repository.load(cmd.claimLineId(), cmd.expectedVersion());
        var events = line.close(cmd.reason(), cmd.actor(), clock);   // guards run here
        repository.save(line);                                       // version check
        outbox.append(events);                                       // same tx
        audit.record(AuditRecord.of(cmd, line, events));             // same tx
        return CommandResult.of(line.id(), line.version());
      }));
}
```

---

## Appendix B — Enforcement matrix (summary)

| Mechanism | Rules |
|---|---|
| Gradle `build-logic` (module-boundary check at configuration time) | ARCH-001, 002, 004, 011, 013, 014; STY-003 |
| ArchUnit (`libs/platform-archtest` `ContractRules`) | ARCH-002…004, 008, 010–013; DOM-001, 003, 010, 011; MON-001; TIME-001/002; SEC-001; AI-003; PER-008; STY-003, 005/006; OBS-002; ERR-002 |
| Error Prone / NullAway / SpotBugs | DOM-006, 007, 009; SM-002; MON-001, 003; ERR-002; STY-002 |
| Custom lint (code, SQL, OpenAPI, AsyncAPI) | SM-007; CFG-002; TIME-001/002/005; PER-003, 006; EVT-002/003; API-001…009; MON-008; AI-005; OBS-003 |
| Test harnesses | SM-005/006; AUD-001; EVT-005; IDEM-002; TST-* |
| Database grants / RLS | ARCH-005; PER-003, 006, 007 |
| PDP / harness runtime | SEC-002/003/004; AI-001/002/009/010/012/014 |
| PR checklist | All [Review] rules |

## Appendix C — Changelog

| Version | Date | Change |
|---|---|---|
| 1.0.0 | 2026-11-01 | Initial contract |
| 1.1.0 | 2026-11-01 | Microservices topology (ADR-0001): added `-service` module to ARCH-001/002; ARCH-006 kernel includes state-machine engine, `PrincipalId`, `IdGenerator`; ARCH-007 reworded; new ARCH-013 (framework-free application), ARCH-014 (platform libraries), ARCH-015 (independent deployability); Spring Boot confined to `-adapters`/`-service`; enforcement matrix adds `build-logic` |

## Appendix D — PR checklist (copy into `.github/pull_request_template.md`)

```
### Traceability
- PRD requirement IDs:
- ADRs (if any):

### Contract checklist
- [ ] New/changed commands: authorized (CRC-SEC-001), audited (CRC-AUD-001), idempotent (CRC-IDEM-001)
- [ ] Lifecycle changes: transition table + tests + regenerated diagram (CRC-SM-001/005)
- [ ] Money: Money type, explicit rounding, exact allocation (CRC-MON-*)
- [ ] Time: injected Clock, explicit effective dates (CRC-TIME-002/007)
- [ ] Events: outbox, envelope, schema compatibility (CRC-EVT-*)
- [ ] PII: classified, encrypted/masked, not in telemetry (CRC-PER-009, CRC-SEC-006)
- [ ] Config over code: no jurisdiction/LOB branches (CRC-CFG-002)
- [ ] AI: gateway only, prompt versioned, evals passed, denylist respected (CRC-AI-*)
```
