# java-insurtech

Production-grade insurance platform — **Claims (primary) + Policy + Billing**, extensible to **Underwriting** —
built as **one microservice per bounded context** ([ADR-0001](docs/adr/0001-microservices-per-bounded-context.md)).

- Product specs: [`spec/`](spec/README.md) — coverage map, 36 PRDs, glossary
- Engineering rules: [`spec/CODE-RULE-CONTRACT.md`](spec/CODE-RULE-CONTRACT.md) — enforced by the build

## Quick start

Requirements: JDK 21, Docker (for the local stack). The Gradle wrapper downloads Gradle itself.

```bash
./gradlew build                                   # format check, Error Prone/NullAway, tests, contract tests
docker compose -f deploy/docker-compose.yml up -d # Postgres (schema per context), Kafka, Schema Registry
./gradlew :services:claims:claims-service:bootRun
curl localhost:8121/actuator/health
```

`./gradlew spotlessApply` formats the code (google-java-format).

## Repository layout

```
build-logic/              Convention plugins: toolchain, Spotless, Error Prone + NullAway, module-boundary checks
gradle/libs.versions.toml Single source of dependency versions
libs/
  shared-kernel/          Money, Factor, Percentage, typed IDs, ActorRef, EffectivePeriod, BusinessDateTime,
                          StateMachine engine, DomainException — JDK only
  platform-application/   Framework-free ports: AuthorizationPort + @Authorized, AuditPort, OutboxPort +
                          EventEnvelope/EventType, TransactionPort, IdempotencyPort, CommandContext
  platform-web/           HTTP edge (auto-configured): Idempotency-Key filter, correlation IDs, Problem Details
  platform-archtest/      The Code Rule Contract as ArchUnit rules (ContractRules)
services/<ctx>/
  <ctx>-api/              Published contract: OpenAPI (resources/openapi), Avro schemas (src/main/avro)
  <ctx>-domain/           Aggregates, value objects, state machines — pure Java
  <ctx>-application/      Use cases and ports — framework-free
  <ctx>-adapters/         HTTP, persistence + Flyway migrations, messaging, external adapters
  <ctx>-service/          Deployable Spring Boot app + contract test
bff/bff-<name>/           Backends-for-frontends — composition only
deploy/                   Local docker-compose stack and generated Postgres init
docs/adr/                 Architecture decision records
tools/scaffold_context.py Generator for new contexts and BFFs
```

## Service catalog

| Context | Service | Port | Tier | PRDs |
|---|---|---|---|---|
| Identity & Access | `identity` | 8101 | DEEP | F01 |
| Audit & Decision Trace | `audit` | 8102 | DEEP | F02 |
| Ledger & Money Movement | `ledger` | 8103 | DEEP | F04 |
| Rules & Decisions | `rules` | 8104 | DEEP | F05 |
| Documents & Evidence | `documents` | 8105 | DEEP | F06 |
| AI Agent Harness | `aiharness` | 8106 | DEEP | F07, AI01–AI07 |
| Product Configuration | `product` | 8110 | DEEP | P01 |
| Policy Lifecycle | `policy` | 8111 | DEEP | P02 |
| Rating & Pricing | `rating` | 8112 | PARALLEL | P03 |
| FNOL Intake | `fnol` | 8120 | PARALLEL | C01 |
| Claims | `claims` | 8121 | DEEP | C02–C05 |
| Underwriting | `underwriting` | 8130 | PARALLEL | U01 |
| Billing & Collections | `billing` | 8140 | PARALLEL | B01 |
| Commissions | `commissions` | 8141 | PARALLEL | B02 |
| Vendors | `vendors` | 8150 | PARALLEL | V01 |
| Notifications & Communications | `notifications` | 8160 | PARALLEL | X04 |
| Privacy, Consent & Retention | `privacy` | 8170 | PARALLEL | R01 |
| Regulatory Reporting | `regreporting` | 8171 | PARALLEL | R02 |
| Fraud, Sanctions & AML | `fraud` | 8172 | PARALLEL | R03 |
| Search & Knowledge | `search` | 8180 | PARALLEL | S01 |
| Customer portal BFF | `bff-customer` | 8201 | PARALLEL | X01 |
| Producer portal BFF | `bff-producer` | 8202 | PARALLEL | X02 |
| Vendor portal BFF | `bff-vendor` | 8203 | PARALLEL | V01 |
| Mobile BFF | `bff-mobile` | 8204 | PARALLEL | X03 |

`eventing` (F03) and `integration` (S02) are platform libraries, not services. Analytics (A01) runs outside the
JVM services and consumes events/CDC only.

## What the build enforces today

| Mechanism | Rules |
|---|---|
| `build-logic` module-boundary check (fails at configuration time) | CRC-ARCH-001 naming, 002 direction, 004 no reaching into other contexts, 011 BFFs, 014 libraries |
| Lombok resolution guard | CRC-STY-003 |
| Spotless (google-java-format) | CRC-STY-001 |
| Error Prone + NullAway (strict on production code) | CRC-STY-002, CRC-DOM-006 |
| `ContractRules` (ArchUnit) in every `-service` and BFF test | CRC-ARCH-002/003/004/013, DOM-003/011, MON-001, TIME-001/002, SEC-001, AI-003, STY-003, OBS-002, ERR-002 |
| `shared-kernel` types and tests (incl. jqwik properties) | CRC-MON-001…004, 006; CRC-SM-001…005; CRC-TIME-004/006 |
| `platform-web` filters | CRC-IDEM-001, CRC-API-003, CRC-API-007 |
| `EventType` / `EventEnvelope` validation | CRC-EVT-002/003 |
| Per-context DB schema + role (local stack) | CRC-ARCH-005 |

## Adding a bounded context

```bash
python3 tools/scaffold_context.py context <name> --title "<Title>" --aggregates <Root>,<Other> \
  --port <port> --prds <IDs> --tier DEEP|PARALLEL --summary "<one paragraph>"
./gradlew build
```

The generator creates the five modules, README, OpenAPI skeleton, platform migration, Spring Boot app and contract
test, and regenerates `deploy/postgres/init`. Add the context to `spec/README.md` §6 and the catalog above.

## Conventions

- Branches are short-lived; commits follow Conventional Commits; PRs use `.github/pull_request_template.md` and
  reference PRD requirement IDs (CRC-DEL-005/006).
- Decisions that change a contract rule need an ADR (`docs/adr/`).
