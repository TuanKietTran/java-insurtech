# FNOL Intake service (`fnol`)

| | |
|---|---|
| Tier | **PARALLEL** |
| Owning PRDs | [C01](../../spec/prd/02-claims/PRD-C01-fnol-intake.md) |
| HTTP port (local) | `8120` |
| Database schema / role | `fnol` / `fnol_svc` |
| Deployable | `:services:fnol:fnol-service` |

Canonical multi-channel loss reports, policy matching, duplicate detection, and conversion to registered claims.

## Modules

| Module | Role | May depend on (enforced by build-logic) |
|---|---|---|
| `fnol-api` | Published contract: OpenAPI (`src/main/resources/openapi`), Avro event schemas (`src/main/avro`) | nothing internal |
| `fnol-domain` | Aggregates, value objects, state machines, invariants | `shared-kernel` |
| `fnol-application` | Use cases (`*Handler` + `@Authorized`), ports | own domain/api, other contexts' `-api`, `platform-application` |
| `fnol-adapters` | HTTP controllers, persistence, Flyway migrations, messaging, external adapters | own modules, other contexts' `-api`, `platform-web` |
| `fnol-service` | Spring Boot application: wiring and configuration only | own modules |

## Owned aggregates

- `LossReport`

Invariants and state machines are specified in the owning PRDs (§5). Generated state-machine diagrams go to
`docs/state-machines/` (CRC-SM-005).

## Run locally

```bash
docker compose -f deploy/docker-compose.yml up -d
./gradlew :services:fnol:fnol-service:bootRun
curl localhost:8120/actuator/health
```

## Runbook

To be written before production (CRC-DOC-002, CODE-RULE-CONTRACT §18 for DEEP contexts).
