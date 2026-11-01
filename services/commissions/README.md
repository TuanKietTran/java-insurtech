# Commissions service (`commissions`)

| | |
|---|---|
| Tier | **PARALLEL** |
| Owning PRDs | [B02](../../spec/prd/04-billing/PRD-B02-commissions.md) |
| HTTP port (local) | `8141` |
| Database schema / role | `commissions` / `commissions_svc` |
| Deployable | `:services:commissions:commissions-service` |

Commission plans, hierarchy splits, chargebacks, license gating, statements and payouts.

## Modules

| Module | Role | May depend on (enforced by build-logic) |
|---|---|---|
| `commissions-api` | Published contract: OpenAPI (`src/main/resources/openapi`), Avro event schemas (`src/main/avro`) | nothing internal |
| `commissions-domain` | Aggregates, value objects, state machines, invariants | `shared-kernel` |
| `commissions-application` | Use cases (`*Handler` + `@Authorized`), ports | own domain/api, other contexts' `-api`, `platform-application` |
| `commissions-adapters` | HTTP controllers, persistence, Flyway migrations, messaging, external adapters | own modules, other contexts' `-api`, `platform-web` |
| `commissions-service` | Spring Boot application: wiring and configuration only | own modules |

## Owned aggregates

- `CommissionPlan`
- `CommissionEntry`
- `Statement`

Invariants and state machines are specified in the owning PRDs (§5). Generated state-machine diagrams go to
`docs/state-machines/` (CRC-SM-005).

## Run locally

```bash
docker compose -f deploy/docker-compose.yml up -d
./gradlew :services:commissions:commissions-service:bootRun
curl localhost:8141/actuator/health
```

## Runbook

To be written before production (CRC-DOC-002, CODE-RULE-CONTRACT §18 for DEEP contexts).
