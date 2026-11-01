# Rules & Decisions service (`rules`)

| | |
|---|---|
| Tier | **DEEP** |
| Owning PRDs | [F05](../../spec/prd/00-foundation/PRD-F05-rules-decision-engine.md) |
| HTTP port (local) | `8104` |
| Database schema / role | `rules` / `rules_svc` |
| Deployable | `:services:rules:rules-service` |

Deterministic, versioned, effective-dated decision tables with explanations, bundled tests, and four-eyes publication.

## Modules

| Module | Role | May depend on (enforced by build-logic) |
|---|---|---|
| `rules-api` | Published contract: OpenAPI (`src/main/resources/openapi`), Avro event schemas (`src/main/avro`) | nothing internal |
| `rules-domain` | Aggregates, value objects, state machines, invariants | `shared-kernel` |
| `rules-application` | Use cases (`*Handler` + `@Authorized`), ports | own domain/api, other contexts' `-api`, `platform-application` |
| `rules-adapters` | HTTP controllers, persistence, Flyway migrations, messaging, external adapters | own modules, other contexts' `-api`, `platform-web` |
| `rules-service` | Spring Boot application: wiring and configuration only | own modules |

## Owned aggregates

- `RuleSet`
- `RuleSetVersion`

Invariants and state machines are specified in the owning PRDs (§5). Generated state-machine diagrams go to
`docs/state-machines/` (CRC-SM-005).

## Run locally

```bash
docker compose -f deploy/docker-compose.yml up -d
./gradlew :services:rules:rules-service:bootRun
curl localhost:8104/actuator/health
```

## Runbook

To be written before production (CRC-DOC-002, CODE-RULE-CONTRACT §18 for DEEP contexts).
