# Privacy, Consent & Retention service (`privacy`)

| | |
|---|---|
| Tier | **PARALLEL** |
| Owning PRDs | [R01](../../spec/prd/07-compliance/PRD-R01-privacy-consent-retention.md) |
| HTTP port (local) | `8170` |
| Database schema / role | `privacy` / `privacy_svc` |
| Deployable | `:services:privacy:privacy-service` |

Classification registry, per-subject keys and crypto-shredding, consent, data subject requests, legal holds, and retention/disposal.

## Modules

| Module | Role | May depend on (enforced by build-logic) |
|---|---|---|
| `privacy-api` | Published contract: OpenAPI (`src/main/resources/openapi`), Avro event schemas (`src/main/avro`) | nothing internal |
| `privacy-domain` | Aggregates, value objects, state machines, invariants | `shared-kernel` |
| `privacy-application` | Use cases (`*Handler` + `@Authorized`), ports | own domain/api, other contexts' `-api`, `platform-application` |
| `privacy-adapters` | HTTP controllers, persistence, Flyway migrations, messaging, external adapters | own modules, other contexts' `-api`, `platform-web` |
| `privacy-service` | Spring Boot application: wiring and configuration only | own modules |

## Owned aggregates

- `DataSubjectRequest`
- `Consent`
- `LegalHold`
- `RetentionClass`

Invariants and state machines are specified in the owning PRDs (§5). Generated state-machine diagrams go to
`docs/state-machines/` (CRC-SM-005).

## Run locally

```bash
docker compose -f deploy/docker-compose.yml up -d
./gradlew :services:privacy:privacy-service:bootRun
curl localhost:8170/actuator/health
```

## Runbook

To be written before production (CRC-DOC-002, CODE-RULE-CONTRACT §18 for DEEP contexts).
