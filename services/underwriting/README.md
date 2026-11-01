# Underwriting service (`underwriting`)

| | |
|---|---|
| Tier | **PARALLEL** |
| Owning PRDs | [U01](../../spec/prd/03-underwriting/PRD-U01-underwriting-risk.md) |
| HTTP port (local) | `8130` |
| Database schema / role | `underwriting` / `underwriting_svc` |
| Deployable | `:services:underwriting:underwriting-service` |

Submission intake, clearance, enrichment, explainable risk scoring, referrals and the underwriter workbench.

## Modules

| Module | Role | May depend on (enforced by build-logic) |
|---|---|---|
| `underwriting-api` | Published contract: OpenAPI (`src/main/resources/openapi`), Avro event schemas (`src/main/avro`) | nothing internal |
| `underwriting-domain` | Aggregates, value objects, state machines, invariants | `shared-kernel` |
| `underwriting-application` | Use cases (`*Handler` + `@Authorized`), ports | own domain/api, other contexts' `-api`, `platform-application` |
| `underwriting-adapters` | HTTP controllers, persistence, Flyway migrations, messaging, external adapters | own modules, other contexts' `-api`, `platform-web` |
| `underwriting-service` | Spring Boot application: wiring and configuration only | own modules |

## Owned aggregates

- `Submission`
- `Referral`
- `RiskScore`

Invariants and state machines are specified in the owning PRDs (§5). Generated state-machine diagrams go to
`docs/state-machines/` (CRC-SM-005).

## Run locally

```bash
docker compose -f deploy/docker-compose.yml up -d
./gradlew :services:underwriting:underwriting-service:bootRun
curl localhost:8130/actuator/health
```

## Runbook

To be written before production (CRC-DOC-002, CODE-RULE-CONTRACT §18 for DEEP contexts).
