# Fraud, Sanctions & AML service (`fraud`)

| | |
|---|---|
| Tier | **PARALLEL** |
| Owning PRDs | [R03](../../spec/prd/07-compliance/PRD-R03-fraud-aml-signals.md) |
| HTTP port (local) | `8172` |
| Database schema / role | `fraud` / `fraud_svc` |
| Deployable | `:services:fraud:fraud-service` |

Sanctions screening before payment release, fraud signals and scores, entity resolution, payment holds, SIU cases, and AML alerts.

## Modules

| Module | Role | May depend on (enforced by build-logic) |
|---|---|---|
| `fraud-api` | Published contract: OpenAPI (`src/main/resources/openapi`), Avro event schemas (`src/main/avro`) | nothing internal |
| `fraud-domain` | Aggregates, value objects, state machines, invariants | `shared-kernel` |
| `fraud-application` | Use cases (`*Handler` + `@Authorized`), ports | own domain/api, other contexts' `-api`, `platform-application` |
| `fraud-adapters` | HTTP controllers, persistence, Flyway migrations, messaging, external adapters | own modules, other contexts' `-api`, `platform-web` |
| `fraud-service` | Spring Boot application: wiring and configuration only | own modules |

## Owned aggregates

- `Signal`
- `ScreeningCheck`
- `Hold`
- `SiuCase`
- `AmlAlert`

Invariants and state machines are specified in the owning PRDs (§5). Generated state-machine diagrams go to
`docs/state-machines/` (CRC-SM-005).

## Run locally

```bash
docker compose -f deploy/docker-compose.yml up -d
./gradlew :services:fraud:fraud-service:bootRun
curl localhost:8172/actuator/health
```

## Runbook

To be written before production (CRC-DOC-002, CODE-RULE-CONTRACT §18 for DEEP contexts).
