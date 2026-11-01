# Product Configuration service (`product`)

| | |
|---|---|
| Tier | **DEEP** |
| Owning PRDs | [P01](../../spec/prd/01-policy/PRD-P01-product-configuration.md) |
| HTTP port (local) | `8110` |
| Database schema / role | `product` / `product_svc` |
| Deployable | `:services:product:product-service` |

Versioned, effective-dated product model: coverages, limits, deductibles, forms, endorsements, risk-object schemas, and claim settings.

## Modules

| Module | Role | May depend on (enforced by build-logic) |
|---|---|---|
| `product-api` | Published contract: OpenAPI (`src/main/resources/openapi`), Avro event schemas (`src/main/avro`) | nothing internal |
| `product-domain` | Aggregates, value objects, state machines, invariants | `shared-kernel` |
| `product-application` | Use cases (`*Handler` + `@Authorized`), ports | own domain/api, other contexts' `-api`, `platform-application` |
| `product-adapters` | HTTP controllers, persistence, Flyway migrations, messaging, external adapters | own modules, other contexts' `-api`, `platform-web` |
| `product-service` | Spring Boot application: wiring and configuration only | own modules |

## Owned aggregates

- `ProductVersion`
- `Product`
- `CoverageDefinition`
- `Form`

Invariants and state machines are specified in the owning PRDs (§5). Generated state-machine diagrams go to
`docs/state-machines/` (CRC-SM-005).

## Run locally

```bash
docker compose -f deploy/docker-compose.yml up -d
./gradlew :services:product:product-service:bootRun
curl localhost:8110/actuator/health
```

## Runbook

To be written before production (CRC-DOC-002, CODE-RULE-CONTRACT §18 for DEEP contexts).
