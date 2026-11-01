# Vendors service (`vendors`)

| | |
|---|---|
| Tier | **PARALLEL** |
| Owning PRDs | [V01](../../spec/prd/05-vendor/PRD-V01-vendor-management.md) |
| HTTP port (local) | `8150` |
| Database schema / role | `vendors` / `vendors_svc` |
| Deployable | `:services:vendors:vendors-service` |

Vendor registry and credentialing, rate agreements, assignment with SLAs, invoice validation, scorecards, and the vendor portal API.

## Modules

| Module | Role | May depend on (enforced by build-logic) |
|---|---|---|
| `vendors-api` | Published contract: OpenAPI (`src/main/resources/openapi`), Avro event schemas (`src/main/avro`) | nothing internal |
| `vendors-domain` | Aggregates, value objects, state machines, invariants | `shared-kernel` |
| `vendors-application` | Use cases (`*Handler` + `@Authorized`), ports | own domain/api, other contexts' `-api`, `platform-application` |
| `vendors-adapters` | HTTP controllers, persistence, Flyway migrations, messaging, external adapters | own modules, other contexts' `-api`, `platform-web` |
| `vendors-service` | Spring Boot application: wiring and configuration only | own modules |

## Owned aggregates

- `Vendor`
- `RateAgreement`
- `VendorAssignment`
- `VendorInvoice`

Invariants and state machines are specified in the owning PRDs (§5). Generated state-machine diagrams go to
`docs/state-machines/` (CRC-SM-005).

## Run locally

```bash
docker compose -f deploy/docker-compose.yml up -d
./gradlew :services:vendors:vendors-service:bootRun
curl localhost:8150/actuator/health
```

## Runbook

To be written before production (CRC-DOC-002, CODE-RULE-CONTRACT §18 for DEEP contexts).
