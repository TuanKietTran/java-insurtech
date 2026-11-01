# Rating & Pricing service (`rating`)

| | |
|---|---|
| Tier | **PARALLEL** |
| Owning PRDs | [P03](../../spec/prd/01-policy/PRD-P03-rating-pricing.md) |
| HTTP port (local) | `8112` |
| Database schema / role | `rating` / `rating_svc` |
| Deployable | `:services:rating:rating-service` |

Deterministic rating plans with step-by-step worksheets, effective-dated rate tables, taxes and fees.

## Modules

| Module | Role | May depend on (enforced by build-logic) |
|---|---|---|
| `rating-api` | Published contract: OpenAPI (`src/main/resources/openapi`), Avro event schemas (`src/main/avro`) | nothing internal |
| `rating-domain` | Aggregates, value objects, state machines, invariants | `shared-kernel` |
| `rating-application` | Use cases (`*Handler` + `@Authorized`), ports | own domain/api, other contexts' `-api`, `platform-application` |
| `rating-adapters` | HTTP controllers, persistence, Flyway migrations, messaging, external adapters | own modules, other contexts' `-api`, `platform-web` |
| `rating-service` | Spring Boot application: wiring and configuration only | own modules |

## Owned aggregates

- `RatingPlan`
- `RatingPlanVersion`
- `RateTable`

Invariants and state machines are specified in the owning PRDs (§5). Generated state-machine diagrams go to
`docs/state-machines/` (CRC-SM-005).

## Run locally

```bash
docker compose -f deploy/docker-compose.yml up -d
./gradlew :services:rating:rating-service:bootRun
curl localhost:8112/actuator/health
```

## Runbook

To be written before production (CRC-DOC-002, CODE-RULE-CONTRACT §18 for DEEP contexts).
