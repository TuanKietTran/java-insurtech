# Documents & Evidence service (`documents`)

| | |
|---|---|
| Tier | **DEEP** |
| Owning PRDs | [F06](../../spec/prd/00-foundation/PRD-F06-document-evidence.md) |
| HTTP port (local) | `8105` |
| Database schema / role | `documents` / `documents_svc` |
| Deployable | `:services:documents:documents-service` |

Immutable, content-addressed documents with provenance and chain of custody, entity links, extractions, requests, templates, and generation.

## Modules

| Module | Role | May depend on (enforced by build-logic) |
|---|---|---|
| `documents-api` | Published contract: OpenAPI (`src/main/resources/openapi`), Avro event schemas (`src/main/avro`) | nothing internal |
| `documents-domain` | Aggregates, value objects, state machines, invariants | `shared-kernel` |
| `documents-application` | Use cases (`*Handler` + `@Authorized`), ports | own domain/api, other contexts' `-api`, `platform-application` |
| `documents-adapters` | HTTP controllers, persistence, Flyway migrations, messaging, external adapters | own modules, other contexts' `-api`, `platform-web` |
| `documents-service` | Spring Boot application: wiring and configuration only | own modules |

## Owned aggregates

- `Document`
- `DocumentLink`
- `Extraction`
- `DocumentRequest`
- `Template`

Invariants and state machines are specified in the owning PRDs (§5). Generated state-machine diagrams go to
`docs/state-machines/` (CRC-SM-005).

## Run locally

```bash
docker compose -f deploy/docker-compose.yml up -d
./gradlew :services:documents:documents-service:bootRun
curl localhost:8105/actuator/health
```

## Runbook

To be written before production (CRC-DOC-002, CODE-RULE-CONTRACT §18 for DEEP contexts).
