# Notifications & Communications service (`notifications`)

| | |
|---|---|
| Tier | **PARALLEL** |
| Owning PRDs | [X04](../../spec/prd/06-experience/PRD-X04-notifications-communications.md) |
| HTTP port (local) | `8160` |
| Database schema / role | `notifications` / `notifications_svc` |
| Deployable | `:services:notifications:notifications-service` |

Template-driven, consent-aware, multi-channel communications with required-notice evidence and conversation threads.

## Modules

| Module | Role | May depend on (enforced by build-logic) |
|---|---|---|
| `notifications-api` | Published contract: OpenAPI (`src/main/resources/openapi`), Avro event schemas (`src/main/avro`) | nothing internal |
| `notifications-domain` | Aggregates, value objects, state machines, invariants | `shared-kernel` |
| `notifications-application` | Use cases (`*Handler` + `@Authorized`), ports | own domain/api, other contexts' `-api`, `platform-application` |
| `notifications-adapters` | HTTP controllers, persistence, Flyway migrations, messaging, external adapters | own modules, other contexts' `-api`, `platform-web` |
| `notifications-service` | Spring Boot application: wiring and configuration only | own modules |

## Owned aggregates

- `Communication`
- `Template`
- `Message`
- `ConversationThread`

Invariants and state machines are specified in the owning PRDs (§5). Generated state-machine diagrams go to
`docs/state-machines/` (CRC-SM-005).

## Run locally

```bash
docker compose -f deploy/docker-compose.yml up -d
./gradlew :services:notifications:notifications-service:bootRun
curl localhost:8160/actuator/health
```

## Runbook

To be written before production (CRC-DOC-002, CODE-RULE-CONTRACT §18 for DEEP contexts).
