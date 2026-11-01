# Search & Knowledge service (`search`)

| | |
|---|---|
| Tier | **PARALLEL** |
| Owning PRDs | [S01](../../spec/prd/09-platform-services/PRD-S01-search-knowledge-base.md) |
| HTTP port (local) | `8180` |
| Database schema / role | `search` / `search_svc` |
| Deployable | `:services:search:search-service` |

Permission-aware operational search and the knowledge base with snapshot-scoped hybrid retrieval and citation anchors.

## Modules

| Module | Role | May depend on (enforced by build-logic) |
|---|---|---|
| `search-api` | Published contract: OpenAPI (`src/main/resources/openapi`), Avro event schemas (`src/main/avro`) | nothing internal |
| `search-domain` | Aggregates, value objects, state machines, invariants | `shared-kernel` |
| `search-application` | Use cases (`*Handler` + `@Authorized`), ports | own domain/api, other contexts' `-api`, `platform-application` |
| `search-adapters` | HTTP controllers, persistence, Flyway migrations, messaging, external adapters | own modules, other contexts' `-api`, `platform-web` |
| `search-service` | Spring Boot application: wiring and configuration only | own modules |

## Owned aggregates

- `KnowledgeItem`
- `Chunk`

Invariants and state machines are specified in the owning PRDs (§5). Generated state-machine diagrams go to
`docs/state-machines/` (CRC-SM-005).

## Run locally

```bash
docker compose -f deploy/docker-compose.yml up -d
./gradlew :services:search:search-service:bootRun
curl localhost:8180/actuator/health
```

## Runbook

To be written before production (CRC-DOC-002, CODE-RULE-CONTRACT §18 for DEEP contexts).
