# ADR-0001 — One microservice per bounded context

| | |
|---|---|
| Status | Accepted |
| Date | 2026-11-01 |
| Contract rules affected | CRC-ARCH-001, 002, 006, 007 (amended); CRC-ARCH-013, 014, 015 (new) — Code Rule Contract 1.1.0 |
| PRDs affected | All (deployment topology); README §6 bounded contexts |

## Context

The Code Rule Contract 1.0 deliberately left topology open: rules had to hold whether contexts were
co-deployed or separate (CRC-ARCH-007). The platform now needs a concrete layout to start building. Forces:

- PARALLEL teams (portals, billing, vendors, AI Agents, analytics) must ship independently of the DEEP core.
- Load is uneven: FNOL registration and rating spike (CAT events, renewals); the ledger needs isolation and
  conservative change control.
- DEEP contexts (ledger, claims, policy, identity, audit, aiharness) need stricter release gates than PARALLEL
  ones (Contract §18).

## Decision

1. **Each bounded context in `spec/README.md` §6 is one independently deployable service**, built from five
   Gradle modules under `services/<ctx>/`: `-api`, `-domain`, `-application`, `-adapters`, `-service`.
   `eventing` and `integration` stay platform libraries rather than services.
2. **Four BFFs** (`bff/bff-customer`, `bff-producer`, `bff-vendor`, `bff-mobile`) compose context APIs for
   clients and hold no domain logic.
3. **Data ownership**: one Postgres schema and one login role per context; no cross-schema access.
4. **Communication**: synchronous calls only through another context's `-api` contract (OpenAPI);
   asynchronous integration through Kafka events published via the transactional outbox (Avro + Schema
   Registry, `BACKWARD_TRANSITIVE`). No distributed transactions; sagas with compensation (CRC-ARCH-009).
5. **Shared code** is limited to `libs/shared-kernel` (value types, state-machine engine) and `libs/platform-*`
   (ports, web edge, contract tests). Platform libraries contain no business logic.
6. **Enforcement is automated**: `build-logic` rejects illegal module dependencies at configuration time, and
   every `-service`/BFF runs `ContractRules` (ArchUnit) in its tests.

## Consequences

- Teams can release a service without coordinating with others, provided `-api` contracts stay compatible
  (CRC-API-002, CRC-EVT-004).
- Cross-context consistency is eventual; flows such as bind → billing → commission and payment approval →
  screening → ledger are sagas (PRD-F03). Process managers and idempotency become mandatory, not optional.
- More operational surface: 24 deployables, per-service observability, health probes and SLOs. Local
  development uses `deploy/docker-compose.yml`.
- A single repository (monorepo) with one Gradle build keeps refactoring and contract checks cheap; services can
  move to separate repositories later without changing module boundaries.

## Alternatives considered

- **Modular monolith first, extract later.** Simplest to operate and fully compatible with the contract's module
  rules. Rejected for now because the owner chose independent deployability from the start; the module layout
  keeps a later consolidation possible.
- **Services by layer (one "domain" service, one "API" service).** Rejected: couples every context's release and
  violates context ownership.
- **Shared database across services.** Rejected: breaks CRC-ARCH-004/005 and makes schema changes cross-team events.
