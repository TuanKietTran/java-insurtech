# Policy Lifecycle service (`policy`)

| | |
|---|---|
| Tier | **DEEP** |
| Owning PRDs | [P02](../../spec/prd/01-policy/PRD-P02-policy-lifecycle.md) |
| HTTP port (local) | `8111` |
| Database schema / role | `policy` / `policy_svc` |
| Deployable | `:services:policy:policy-service` |

Quote, bind, issue, endorse (incl. out-of-sequence), cancel, reinstate, renew; bitemporal policy versions and the CoverageSnapshot API for claims.

## Modules

| Module | Role | May depend on (enforced by build-logic) |
|---|---|---|
| `policy-api` | Published contract: OpenAPI (`src/main/resources/openapi`), Avro event schemas (`src/main/avro`) | nothing internal |
| `policy-domain` | Aggregates, value objects, state machines, invariants | `shared-kernel` |
| `policy-application` | Use cases (`*Handler` + `@Authorized`), ports | own domain/api, other contexts' `-api`, `platform-application` |
| `policy-adapters` | HTTP controllers, persistence, Flyway migrations, messaging, external adapters | own modules, other contexts' `-api`, `platform-web` |
| `policy-service` | Spring Boot application: wiring and configuration only | own modules |

## Owned aggregates

- `PolicyTerm`
- `Quote`
- `Policy`
- `PolicyTransaction`
- `PolicyVersion`

Invariants and state machines are specified in the owning PRDs (§5). Generated state-machine diagrams go to
`docs/state-machines/` (CRC-SM-005).

## Run locally

```bash
docker compose -f deploy/docker-compose.yml up -d
./gradlew :services:policy:policy-service:bootRun
curl localhost:8111/actuator/health
```

## Runbook

To be written before production (CRC-DOC-002, CODE-RULE-CONTRACT §18 for DEEP contexts).
