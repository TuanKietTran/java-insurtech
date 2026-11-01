# PRD-F03 — Event Backbone, Idempotency & Process Orchestration

| Field | Value |
|---|---|
| ID | PRD-F03 |
| Domain | Foundation |
| Tier | **DEEP** |
| Status | Draft v0.1 |
| Owner | Platform Core (TBD) |
| Depends on | F01 (service identity) |
| Consumed by | Every PRD |
| Key contract rules | CRC-EVT-001…010; CRC-IDEM-001…006; CRC-ARCH-009; CRC-SM-008 |

## 1. Problem Statement

Insurance workflows span many contexts: binding a policy must create a billing account, a commission schedule, documents, and notifications; approving a claim payment must post to the ledger, screen sanctions, and notify the payee. Done with synchronous calls or shared databases, these chains create double payments on retries, lost side effects on partial failures, and tight coupling that blocks parallel development. The platform needs a reliable event backbone with exactly-once *effects*, explicit sagas, and a uniform idempotency model.

## 2. Goals

1. **No lost or duplicated side effects**: zero duplicate financial effects caused by retries or redelivery (property-tested and monitored).
2. **Decoupled teams**: parallel contexts integrate only through published events and APIs; schema-breaking changes are caught in CI before merge.
3. **Observable flows**: any business flow (e.g., bind → bill → commission) is traceable end-to-end with one correlation ID.
4. **Recoverability**: any consumer's projection can be rebuilt by replay; DLQ items are replayable after fix with no manual DB edits.

## 3. Non-Goals

- **Event sourcing as a mandate.** Aggregates are stored as state + outbox events; event sourcing MAY be chosen per aggregate by ADR (the ledger is naturally append-only).
- **Partner-facing webhooks.** Covered by S02 (built on this backbone).
- **Analytics streaming platform.** A01 consumes events/CDC; analytics infrastructure is owned there.
- **Generic BPMN engine.** Process managers are domain code; a durable timer/workflow runtime may support them but never owns domain state.

## 4. Personas & User Stories

**Domain developer**
- As a domain developer, I want a library that writes events to an outbox in my transaction so that I never publish an event for a rolled-back change.
- As a consumer developer, I want inbox deduplication handled by the framework so that my handler runs its side effect once per event.
- As a developer, I want CI to reject my schema change if it breaks a consumer so that I find out before production.

**SRE / support engineer**
- As an SRE, I want a DLQ console showing failed events with error, payload (masked), and a replay button so that I can recover after a fix.
- As a support engineer, I want to look up a correlation ID and see every event, command, and saga step in the flow.

**API client (partner, portal)**
- As an API client, I want to retry a timed-out "submit payment" request safely with the same Idempotency-Key and get the original result.

**Edge cases**
- A consumer receives `aggregateVersion` 7 before 6 → it must park/reorder or fetch current state, never apply stale data.
- A saga step fails permanently → compensation runs and a work item is raised for a human.

## 5. Domain Model & Invariants

**Event envelope** — see CRC-EVT-003 (mandatory fields).

**Event classes**
| Class | Purpose | Visibility |
|---|---|---|
| Domain event | Fact inside a context | Internal to context |
| Integration event | Published fact for other contexts | Registered schema, catalogued |
| Command message | Async command to a specific context (e.g., saga step) | Point-to-point topic; never on event topics |

**Idempotency record**: (tenantId, principalId, operation, key) → requestHash, status (`IN_PROGRESS`, `COMPLETED`, `FAILED_RETRYABLE`), response snapshot, createdAt, expiresAt (∞ for financial).

**Process manager / saga**: id, type, correlationId, state (explicit state machine per CRC-SM-001), steps with status, compensation log, deadlines.

**Invariants**
- INV-F03-1: An integration event is published iff the producing transaction committed (outbox).
- INV-F03-2: For each (consumer, eventId), side effects are committed at most once (inbox in same tx).
- INV-F03-3: Events of one aggregate are delivered in `aggregateVersion` order within a partition.
- INV-F03-4: A financial idempotency key maps to exactly one outcome forever.
- INV-F03-5: Saga state transitions are audited and each step is idempotent with a derived key (CRC-IDEM-004).

## 6. Requirements

### P0 — Must-have

**F03-P0-01 Outbox library + relay.** Transactional outbox for Postgres with a relay (polling or log-based CDC) to Kafka; ordering per aggregate; metrics on lag.
*Acceptance:* Given a command transaction that rolls back, no event is published. Relay lag p99 < 2 s under nominal load.

**F03-P0-02 Inbox/dedup library.** Consumer framework with inbox table, retries with backoff, DLQ routing, version-gap detection.
*Acceptance:* redelivering the same event 10 times produces one side effect (replay test, CRC-TST-011).

**F03-P0-03 Schema registry & CI compatibility.** Avro schemas in `<ctx>-api`, registered on merge; `BACKWARD_TRANSITIVE` enforced; event catalog (AsyncAPI) generated per release.

**F03-P0-04 HTTP idempotency middleware.** Implements CRC-IDEM-001…003 uniformly for all contexts, including concurrent duplicate requests (second request waits or receives `409 in-progress`).
*Acceptance:*
- Same key + same body → identical status and body returned, no new side effect.
- Same key + different body → `422 idempotency-key-reuse`.
- Financial operations: key reuse detected even after years (permanent constraint).

**F03-P0-05 Process manager toolkit.** Library for sagas with persisted state machine, step timeouts, compensation, and escalation to work items (C03). Durable timers for SLA/expiry commands (CRC-SM-008).

**F03-P0-06 Correlation & tracing.** Correlation and causation IDs and `traceparent` flow through HTTP → outbox → broker → consumer → next command.

**F03-P0-07 DLQ operations.** Console/API to inspect (masked), replay, or discard (with reason and audit) DLQ messages; alerting on DLQ depth.

**F03-P0-08 Tenant-aware topics.** Tenant in envelope and partition strategy that supports per-tenant replay and throttling; noisy-tenant protection.

### P1 — Fast follow
- **F03-P1-01** Projection rebuild tooling (replay from offset/time for a consumer group into a fresh read model).
- **F03-P1-02** Event archive to object storage for long-term replay beyond broker retention.
- **F03-P1-03** Consumer-lag SLO dashboards per context.
- **F03-P1-04** Flow explorer UI: correlation ID → sequence diagram of events and commands.

### P2 — Future
- **F03-P2-01** Cross-region replication with tenant residency constraints.
- **F03-P2-02** Selective event-sourced aggregates where audit/replay value justifies it.

## 7. Interfaces & Events
- Libraries: `eventing-outbox`, `eventing-inbox`, `eventing-saga`, `idempotency-http`.
- Platform events: `eventing.dlq.message-parked.v1`, `eventing.saga.compensated.v1`, `eventing.saga.escalated.v1`.
- Canonical flows built on this (documented in owning PRDs): Bind → Billing account/invoice (B01) → Commission schedule (B02) → Documents (F06) → Notifications (X04); Payment approval (C05) → Sanctions screen (R03) → Ledger post & release (F04) → Notification (X04).

## 8. AI Integration Points
- AI tool calls that create Proposals use derived idempotency keys so a retried run does not create duplicate proposals.
- AI runs can be triggered by events (e.g., `claims.claim.registered.v1` → triage recommendation) through standard consumers.

## 9. Audit, Security & Compliance
- Saga transitions and DLQ discards are audited.
- Payload PII minimization enforced by schema classification checks (CRC-EVT-007); DLQ views masked.
- Topic ACLs per context (producer owns its topics).

## 10. Success Metrics

| Metric | Type | Target (hypothesis) |
|---|---|---|
| Duplicate financial effects from redelivery | Lagging | 0 |
| Outbox relay lag p99 | Leading | < 2 s |
| Schema-breaking changes reaching main | Leading | 0 |
| DLQ mean time to resolution (DEEP contexts) | Lagging | < 4 business hours |
| Flows traceable by correlation ID (sampled) | Leading | 100% |

## 11. Open Questions
- [Engineering] [Blocking] Outbox relay: polling vs. log-based CDC (Debezium) — throughput and ops trade-off.
- [Engineering] [Blocking] Durable timers: in-house scheduler table vs. workflow engine (Temporal) — ADR.
- [Engineering] [Non-blocking] Avro vs. Protobuf (baseline Avro; revisit if gRPC adopted).

## 12. Dependencies & Phasing
- **First** (week-zero foundation): envelope, outbox/inbox, idempotency middleware, schema CI.
- **Then**: saga toolkit and durable timers (needed by P02 cancellations, B01 delinquency, C02 SLA timers).
- **Parallel**: DLQ console, flow explorer, archive.
