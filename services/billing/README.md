# Billing & Collections service (`billing`)

| | |
|---|---|
| Tier | **PARALLEL** |
| Owning PRDs | [B01](../../spec/prd/04-billing/PRD-B01-premium-billing-collections.md) |
| HTTP port (local) | `8140` |
| Database schema / role | `billing` / `billing_svc` |
| Deployable | `:services:billing:billing-service` |

Billing accounts and plans, invoices, payments via PSP tokens, allocation, delinquency, refunds, write-offs, and agency bill.

## Modules

| Module | Role | May depend on (enforced by build-logic) |
|---|---|---|
| `billing-api` | Published contract: OpenAPI (`src/main/resources/openapi`), Avro event schemas (`src/main/avro`) | nothing internal |
| `billing-domain` | Aggregates, value objects, state machines, invariants | `shared-kernel` |
| `billing-application` | Use cases (`*Handler` + `@Authorized`), ports | own domain/api, other contexts' `-api`, `platform-application` |
| `billing-adapters` | HTTP controllers, persistence, Flyway migrations, messaging, external adapters | own modules, other contexts' `-api`, `platform-web` |
| `billing-service` | Spring Boot application: wiring and configuration only | own modules |

## Owned aggregates

- `BillingAccount`
- `Invoice`
- `ReceiptAllocation`
- `Refund`

Invariants and state machines are specified in the owning PRDs (§5). Generated state-machine diagrams go to
`docs/state-machines/` (CRC-SM-005).

## Run locally

```bash
docker compose -f deploy/docker-compose.yml up -d
./gradlew :services:billing:billing-service:bootRun
curl localhost:8140/actuator/health
```

## Runbook

To be written before production (CRC-DOC-002, CODE-RULE-CONTRACT §18 for DEEP contexts).
