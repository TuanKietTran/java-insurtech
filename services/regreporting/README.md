# Regulatory Reporting service (`regreporting`)

| | |
|---|---|
| Tier | **PARALLEL** |
| Owning PRDs | [R02](../../spec/prd/07-compliance/PRD-R02-regulatory-reporting.md) |
| HTTP port (local) | `8171` |
| Database schema / role | `regreporting` / `regreporting_svc` |
| Deployable | `:services:regreporting:regreporting-service` |

Versioned report definitions over bitemporal data, reproducible runs, review and sign-off, submissions, and bordereaux.

## Modules

| Module | Role | May depend on (enforced by build-logic) |
|---|---|---|
| `regreporting-api` | Published contract: OpenAPI (`src/main/resources/openapi`), Avro event schemas (`src/main/avro`) | nothing internal |
| `regreporting-domain` | Aggregates, value objects, state machines, invariants | `shared-kernel` |
| `regreporting-application` | Use cases (`*Handler` + `@Authorized`), ports | own domain/api, other contexts' `-api`, `platform-application` |
| `regreporting-adapters` | HTTP controllers, persistence, Flyway migrations, messaging, external adapters | own modules, other contexts' `-api`, `platform-web` |
| `regreporting-service` | Spring Boot application: wiring and configuration only | own modules |

## Owned aggregates

- `ReportRun`
- `ReportDefinition`
- `Submission`

Invariants and state machines are specified in the owning PRDs (§5). Generated state-machine diagrams go to
`docs/state-machines/` (CRC-SM-005).

## Run locally

```bash
docker compose -f deploy/docker-compose.yml up -d
./gradlew :services:regreporting:regreporting-service:bootRun
curl localhost:8171/actuator/health
```

## Runbook

To be written before production (CRC-DOC-002, CODE-RULE-CONTRACT §18 for DEEP contexts).
