# AI Agent Harness service (`aiharness`)

| | |
|---|---|
| Tier | **DEEP** |
| Owning PRDs | [F07](../../spec/prd/00-foundation/PRD-F07-agent-harness-core.md), [AI01](../../spec/prd/10-ai-agents/PRD-AI01-fnol-extraction-agent.md), [AI02](../../spec/prd/10-ai-agents/PRD-AI02-document-understanding-agent.md), [AI03](../../spec/prd/10-ai-agents/PRD-AI03-coverage-qa-rag-agent.md), [AI04](../../spec/prd/10-ai-agents/PRD-AI04-triage-agent.md), [AI05](../../spec/prd/10-ai-agents/PRD-AI05-fraud-summarization-agent.md), [AI06](../../spec/prd/10-ai-agents/PRD-AI06-communication-drafting-agent.md), [AI07](../../spec/prd/10-ai-agents/PRD-AI07-policy-wording-endorsement-agent.md) |
| HTTP port (local) | `8106` |
| Database schema / role | `aiharness` / `aiharness_svc` |
| Deployable | `:services:aiharness:aiharness-service` |

AI Agent registry, permissioned tools, model gateway, run traces, proposals and approval gates, budgets, guardrails, eval harness, and kill switches. Hosts AI01-AI07.

## Modules

| Module | Role | May depend on (enforced by build-logic) |
|---|---|---|
| `aiharness-api` | Published contract: OpenAPI (`src/main/resources/openapi`), Avro event schemas (`src/main/avro`) | nothing internal |
| `aiharness-domain` | Aggregates, value objects, state machines, invariants | `shared-kernel` |
| `aiharness-application` | Use cases (`*Handler` + `@Authorized`), ports | own domain/api, other contexts' `-api`, `platform-application` |
| `aiharness-adapters` | HTTP controllers, persistence, Flyway migrations, messaging, external adapters | own modules, other contexts' `-api`, `platform-web` |
| `aiharness-service` | Spring Boot application: wiring and configuration only | own modules |

## Owned aggregates

- `AiAgentDefinition`
- `Tool`
- `Run`
- `Proposal`
- `EvalSuite`

Invariants and state machines are specified in the owning PRDs (§5). Generated state-machine diagrams go to
`docs/state-machines/` (CRC-SM-005).

## Run locally

```bash
docker compose -f deploy/docker-compose.yml up -d
./gradlew :services:aiharness:aiharness-service:bootRun
curl localhost:8106/actuator/health
```

## Runbook

To be written before production (CRC-DOC-002, CODE-RULE-CONTRACT §18 for DEEP contexts).
