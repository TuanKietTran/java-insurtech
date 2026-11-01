# Claims service (`claims`)

| | |
|---|---|
| Tier | **DEEP** |
| Owning PRDs | [C02](../../spec/prd/02-claims/PRD-C02-claim-lifecycle-state-machine.md), [C03](../../spec/prd/02-claims/PRD-C03-assignment-triage-work.md), [C04](../../spec/prd/02-claims/PRD-C04-coverage-liability.md), [C05](../../spec/prd/02-claims/PRD-C05-claim-financials.md) |
| HTTP port (local) | `8121` |
| Database schema / role | `claims` / `claims_svc` |
| Deployable | `:services:claims:claims-service` |

Claim and claim-line state machines, triage and work management, coverage and liability determination, reserves, settlements, payments and recoveries.

## Modules

| Module | Role | May depend on (enforced by build-logic) |
|---|---|---|
| `claims-api` | Published contract: OpenAPI (`src/main/resources/openapi`), Avro event schemas (`src/main/avro`) | nothing internal |
| `claims-domain` | Aggregates, value objects, state machines, invariants | `shared-kernel` |
| `claims-application` | Use cases (`*Handler` + `@Authorized`), ports | own domain/api, other contexts' `-api`, `platform-application` |
| `claims-adapters` | HTTP controllers, persistence, Flyway migrations, messaging, external adapters | own modules, other contexts' `-api`, `platform-web` |
| `claims-service` | Spring Boot application: wiring and configuration only | own modules |

## Owned aggregates

- `Claim`
- `ClaimLine`
- `Party`
- `WorkItem`
- `CoveragePosition`
- `Reserve`
- `Recovery`

Invariants and state machines are specified in the owning PRDs (§5). Generated state-machine diagrams go to
`docs/state-machines/` (CRC-SM-005).

## Run locally

```bash
docker compose -f deploy/docker-compose.yml up -d
./gradlew :services:claims:claims-service:bootRun
curl localhost:8121/actuator/health
```

## Runbook

To be written before production (CRC-DOC-002, CODE-RULE-CONTRACT §18 for DEEP contexts).
