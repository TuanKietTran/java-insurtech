# Audit & Decision Trace service (`audit`)

| | |
|---|---|
| Tier | **DEEP** |
| Owning PRDs | [F02](../../spec/prd/00-foundation/PRD-F02-audit-decision-trace.md) |
| HTTP port (local) | `8102` |
| Database schema / role | `audit` / `audit_svc` |
| Deployable | `:services:audit:audit-service` |

Append-only, tamper-evident audit and decision records; timelines, the "why" view, point-in-time reconstruction, and evidence packs.

## Modules

| Module | Role | May depend on (enforced by build-logic) |
|---|---|---|
| `audit-api` | Published contract: OpenAPI (`src/main/resources/openapi`), Avro event schemas (`src/main/avro`) | nothing internal |
| `audit-domain` | Aggregates, value objects, state machines, invariants | `shared-kernel` |
| `audit-application` | Use cases (`*Handler` + `@Authorized`), ports | own domain/api, other contexts' `-api`, `platform-application` |
| `audit-adapters` | HTTP controllers, persistence, Flyway migrations, messaging, external adapters | own modules, other contexts' `-api`, `platform-web` |
| `audit-service` | Spring Boot application: wiring and configuration only | own modules |

## Owned aggregates

- `AuditRecord`
- `DecisionRecord`

Invariants and state machines are specified in the owning PRDs (§5). Generated state-machine diagrams go to
`docs/state-machines/` (CRC-SM-005).

## Run locally

```bash
docker compose -f deploy/docker-compose.yml up -d
./gradlew :services:audit:audit-service:bootRun
curl localhost:8102/actuator/health
```

## Runbook

To be written before production (CRC-DOC-002, CODE-RULE-CONTRACT §18 for DEEP contexts).
