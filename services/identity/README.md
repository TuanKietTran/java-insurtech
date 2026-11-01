# Identity & Access service (`identity`)

| | |
|---|---|
| Tier | **DEEP** |
| Owning PRDs | [F01](../../spec/prd/00-foundation/PRD-F01-identity-access-tenancy.md) |
| HTTP port (local) | `8101` |
| Database schema / role | `identity` / `identity_svc` |
| Deployable | `:services:identity:identity-service` |

Tenants, organizations, principals (human, service, external, AI Agent), roles, grants, authority limits, segregation of duties, and the Policy Decision Point.

## Modules

| Module | Role | May depend on (enforced by build-logic) |
|---|---|---|
| `identity-api` | Published contract: OpenAPI (`src/main/resources/openapi`), Avro event schemas (`src/main/avro`) | nothing internal |
| `identity-domain` | Aggregates, value objects, state machines, invariants | `shared-kernel` |
| `identity-application` | Use cases (`*Handler` + `@Authorized`), ports | own domain/api, other contexts' `-api`, `platform-application` |
| `identity-adapters` | HTTP controllers, persistence, Flyway migrations, messaging, external adapters | own modules, other contexts' `-api`, `platform-web` |
| `identity-service` | Spring Boot application: wiring and configuration only | own modules |

## Owned aggregates

- `Principal`
- `Tenant`
- `Organization`
- `Role`
- `Grant`
- `AuthorityLimit`

Invariants and state machines are specified in the owning PRDs (§5). Generated state-machine diagrams go to
`docs/state-machines/` (CRC-SM-005).

## Run locally

```bash
docker compose -f deploy/docker-compose.yml up -d
./gradlew :services:identity:identity-service:bootRun
curl localhost:8101/actuator/health
```

## Runbook

To be written before production (CRC-DOC-002, CODE-RULE-CONTRACT §18 for DEEP contexts).
