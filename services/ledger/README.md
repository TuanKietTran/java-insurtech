# Ledger & Money Movement service (`ledger`)

| | |
|---|---|
| Tier | **DEEP** |
| Owning PRDs | [F04](../../spec/prd/00-foundation/PRD-F04-financial-ledger.md) |
| HTTP port (local) | `8103` |
| Database schema / role | `ledger` / `ledger_svc` |
| Deployable | `:services:ledger:ledger-service` |

Double-entry, append-only insurance sub-ledger; posting rules, balances, payment orders with approval/screening, receipts, reconciliation, and GL export.

## Modules

| Module | Role | May depend on (enforced by build-logic) |
|---|---|---|
| `ledger-api` | Published contract: OpenAPI (`src/main/resources/openapi`), Avro event schemas (`src/main/avro`) | nothing internal |
| `ledger-domain` | Aggregates, value objects, state machines, invariants | `shared-kernel` |
| `ledger-application` | Use cases (`*Handler` + `@Authorized`), ports | own domain/api, other contexts' `-api`, `platform-application` |
| `ledger-adapters` | HTTP controllers, persistence, Flyway migrations, messaging, external adapters | own modules, other contexts' `-api`, `platform-web` |
| `ledger-service` | Spring Boot application: wiring and configuration only | own modules |

## Owned aggregates

- `JournalEntry`
- `Account`
- `PostingRule`
- `AccountingPeriod`
- `PaymentOrder`
- `Receipt`

Invariants and state machines are specified in the owning PRDs (§5). Generated state-machine diagrams go to
`docs/state-machines/` (CRC-SM-005).

## Run locally

```bash
docker compose -f deploy/docker-compose.yml up -d
./gradlew :services:ledger:ledger-service:bootRun
curl localhost:8103/actuator/health
```

## Runbook

To be written before production (CRC-DOC-002, CODE-RULE-CONTRACT §18 for DEEP contexts).
