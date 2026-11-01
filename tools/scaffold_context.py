#!/usr/bin/env python3
"""Scaffold a bounded-context microservice or a BFF that follows spec/CODE-RULE-CONTRACT.md.

A context gets five modules under services/<ctx>/ (CRC-ARCH-001):
  <ctx>-api          published contract: OpenAPI + Avro schemas (depends on nothing internal)
  <ctx>-domain       pure Java model (shared-kernel only)
  <ctx>-application  framework-free use cases and ports
  <ctx>-adapters     HTTP, persistence (Flyway), messaging
  <ctx>-service      deployable Spring Boot application + contract test

Examples:
  python3 tools/scaffold_context.py context claims --title Claims --aggregates Claim,ClaimLine \
      --port 8121 --prds C02,C03 --tier DEEP --summary "Claim lifecycle ..."
  python3 tools/scaffold_context.py bff customer --title "Customer portal" --port 8201 --prds X01 \
      --summary "Backend for the customer portal."

Existing files are never overwritten. After scaffolding, run ./gradlew build.
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ROLES = ("api", "domain", "application", "adapters", "service")


def write(path: Path, content: str) -> None:
    if path.exists():
        print(f"  skip   {path.relative_to(ROOT)} (exists)")
        return
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")
    print(f"  create {path.relative_to(ROOT)}")


def pascal(text: str) -> str:
    return "".join(part[:1].upper() + part[1:] for part in re.split(r"[^A-Za-z0-9]+", text) if part)


def prd_links(prds: list[str], depth: int) -> str:
    links = []
    for prd in prds:
        matches = sorted((ROOT / "spec" / "prd").glob(f"*/PRD-{prd}-*.md"))
        if matches:
            rel = "../" * depth + matches[0].relative_to(ROOT).as_posix()
            links.append(f"[{prd}]({rel})")
        else:
            links.append(prd)
    return ", ".join(links)


def javadoc_prds(prds: list[str]) -> str:
    return ", ".join(f"PRD-{p}" for p in prds)


# --------------------------------------------------------------------------------------------------
# Bounded context
# --------------------------------------------------------------------------------------------------


def scaffold_context(a: argparse.Namespace) -> None:
    ctx = a.name
    if not re.fullmatch(r"[a-z][a-z0-9]*", ctx):
        sys.exit("context names are lowercase alphanumerics (they become Java package names)")
    cls = a.pascal or pascal(ctx)
    aggregates = [x.strip() for x in a.aggregates.split(",") if x.strip()]
    prds = [x.strip() for x in a.prds.split(",") if x.strip()]
    root_id = f"{aggregates[0]}Id"
    pkg = f"com.insurtech.{ctx}"
    pkg_path = pkg.replace(".", "/")
    base = ROOT / "services" / ctx
    print(f"Scaffolding context '{ctx}' at {base.relative_to(ROOT)}")

    # README (CRC-DOC-002)
    aggregate_list = "\n".join(f"- `{x}`" for x in aggregates)
    write(
        base / "README.md",
        f"""# {a.title} service (`{ctx}`)

| | |
|---|---|
| Tier | **{a.tier}** |
| Owning PRDs | {prd_links(prds, 2)} |
| HTTP port (local) | `{a.port}` |
| Database schema / role | `{ctx}` / `{ctx}_svc` |
| Deployable | `:services:{ctx}:{ctx}-service` |

{a.summary}

## Modules

| Module | Role | May depend on (enforced by build-logic) |
|---|---|---|
| `{ctx}-api` | Published contract: OpenAPI (`src/main/resources/openapi`), Avro event schemas (`src/main/avro`) | nothing internal |
| `{ctx}-domain` | Aggregates, value objects, state machines, invariants | `shared-kernel` |
| `{ctx}-application` | Use cases (`*Handler` + `@Authorized`), ports | own domain/api, other contexts' `-api`, `platform-application` |
| `{ctx}-adapters` | HTTP controllers, persistence, Flyway migrations, messaging, external adapters | own modules, other contexts' `-api`, `platform-web` |
| `{ctx}-service` | Spring Boot application: wiring and configuration only | own modules |

## Owned aggregates

{aggregate_list}

Invariants and state machines are specified in the owning PRDs (§5). Generated state-machine diagrams go to
`docs/state-machines/` (CRC-SM-005).

## Run locally

```bash
docker compose -f deploy/docker-compose.yml up -d
./gradlew :services:{ctx}:{ctx}-service:bootRun
curl localhost:{a.port}/actuator/health
```

## Runbook

To be written before production (CRC-DOC-002, CODE-RULE-CONTRACT §18 for DEEP contexts).
""",
    )

    # build files
    write(base / f"{ctx}-api/build.gradle.kts", 'plugins {\n    id("insurtech.api-conventions")\n}\n')
    write(base / f"{ctx}-domain/build.gradle.kts", 'plugins {\n    id("insurtech.domain-conventions")\n}\n')
    write(
        base / f"{ctx}-application/build.gradle.kts",
        f"""plugins {{
    id("insurtech.application-conventions")
}}

dependencies {{
    api(project(":services:{ctx}:{ctx}-domain"))
    api(project(":services:{ctx}:{ctx}-api"))
}}
""",
    )
    write(
        base / f"{ctx}-adapters/build.gradle.kts",
        f"""plugins {{
    id("insurtech.adapters-conventions")
}}

dependencies {{
    implementation(project(":services:{ctx}:{ctx}-application"))
}}
""",
    )
    write(
        base / f"{ctx}-service/build.gradle.kts",
        f"""plugins {{
    id("insurtech.service-conventions")
}}

dependencies {{
    implementation(project(":services:{ctx}:{ctx}-adapters"))
}}
""",
    )

    # api
    write(
        base / f"{ctx}-api/src/main/java/{pkg_path}/api/package-info.java",
        f"""/**
 * Published contract of the {{@code {ctx}}} bounded context ({javadoc_prds(prds)}).
 *
 * <p>Other contexts and BFFs depend only on this module (CRC-ARCH-004). It holds client DTOs; the
 * OpenAPI specification and Avro event schemas live under {{@code src/main/resources/openapi}} and
 * {{@code src/main/avro}}.
 */
package {pkg}.api;
""",
    )
    write(base / f"{ctx}-api/src/main/avro/.gitkeep", "")
    write(
        base / f"{ctx}-api/src/main/resources/openapi/{ctx}-v1.yaml",
        f"""openapi: 3.1.0
info:
  title: {a.title} API
  version: 1.0.0
  description: |
    Published contract of the `{ctx}` bounded context ({", ".join(prds)}).

    - Contract-first (CRC-API-001); URI-versioned (CRC-API-002).
    - Lifecycle changes are command resources such as `POST /v1/<resource>/{{id}}/close` (CRC-API-004).
    - Mutating operations require `Idempotency-Key` (CRC-IDEM-001) and `If-Match` on aggregates (CRC-SM-009).
    - Errors are RFC 9457 Problem Details (CRC-API-003); money is a string amount plus currency (CRC-MON-008).
servers:
  - url: http://localhost:{a.port}
paths: {{}}
components:
  parameters:
    IdempotencyKey:
      name: Idempotency-Key
      in: header
      required: true
      schema:
        type: string
        minLength: 8
        maxLength: 128
    IfMatch:
      name: If-Match
      in: header
      required: true
      schema:
        type: string
  schemas:
    Money:
      type: object
      required: [amount, currency]
      properties:
        amount:
          type: string
          pattern: '^-?\\d+(\\.\\d+)?$'
        currency:
          type: string
          pattern: '^[A-Z]{{3}}$'
    Problem:
      type: object
      required: [type, status, code]
      properties:
        type:
          type: string
          format: uri
        title:
          type: string
        status:
          type: integer
        detail:
          type: string
        code:
          type: string
        reasons:
          type: array
          items:
            type: string
""",
    )

    # domain
    write(
        base / f"{ctx}-domain/src/main/java/{pkg_path}/domain/package-info.java",
        f"""/**
 * Domain model of the {{@code {ctx}}} bounded context ({javadoc_prds(prds)}).
 *
 * <p>Pure Java (CRC-ARCH-003): no frameworks, no I/O, no direct clock or randomness (CRC-DOM-010).
 * Lifecycle aggregates declare their transitions with {{@code
 * com.insurtech.kernel.statemachine.StateMachine}} (CRC-SM-001).
 */
package {pkg}.domain;
""",
    )
    write(
        base / f"{ctx}-domain/src/main/java/{pkg_path}/domain/{root_id}.java",
        f"""package {pkg}.domain;

import java.util.Objects;
import java.util.UUID;

/** Identifier of a {{@code {aggregates[0]}}} aggregate (CRC-DOM-001). */
public record {root_id}(UUID value) {{

  public {root_id} {{
    Objects.requireNonNull(value, "value");
  }}

  public static {root_id} of(String value) {{
    return new {root_id}(UUID.fromString(value));
  }}

  @Override
  public String toString() {{
    return value.toString();
  }}
}}
""",
    )
    write(
        base / f"{ctx}-domain/src/test/java/{pkg_path}/domain/{root_id}Test.java",
        f"""package {pkg}.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class {root_id}Test {{

  @Test
  void roundTripsThroughItsStringForm() {{
    UUID raw = UUID.fromString("7d444840-9dc0-11d1-b245-5ffdce74fad2");

    assertThat({root_id}.of(raw.toString())).isEqualTo(new {root_id}(raw));
  }}
}}
""",
    )

    # application
    write(
        base / f"{ctx}-application/src/main/java/{pkg_path}/application/package-info.java",
        f"""/**
 * Use cases of the {{@code {ctx}}} bounded context ({javadoc_prds(prds)}).
 *
 * <p>Framework-free (CRC-ARCH-013). Each command handler authorizes first ({{@code @Authorized}},
 * CRC-SEC-001), runs in one {{@code TransactionPort}} transaction, and persists state, outbox events
 * and the audit record together (CRC-SM-006, CRC-AUD-001), behind {{@code IdempotencyPort}}
 * (CRC-IDEM-002).
 */
package {pkg}.application;
""",
    )

    # adapters
    write(
        base / f"{ctx}-adapters/src/main/java/{pkg_path}/adapters/package-info.java",
        f"""/**
 * Adapters of the {{@code {ctx}}} bounded context: HTTP controllers implementing {{@code
 * {ctx}-v1.yaml}}, persistence in schema {{@code {ctx}}}, outbox relay and event consumers, and
 * external-system adapters (CRC-ARCH-008).
 */
package {pkg}.adapters;
""",
    )
    write(
        base / f"{ctx}-adapters/src/main/resources/db/migration/{ctx}/V1__platform_tables.sql",
        f"""-- Platform tables every service owns in its own schema ({ctx}).
-- Outbox (CRC-EVT-001), inbox (CRC-EVT-005), idempotency records (CRC-IDEM-002/003), tenant RLS (CRC-PER-003).
-- Migrations are forward-only and immutable once merged (CRC-PER-002).

CREATE TABLE outbox_event (
    event_id          uuid        PRIMARY KEY,
    tenant_id         uuid        NOT NULL,
    event_type        text        NOT NULL,
    schema_version    integer     NOT NULL,
    aggregate_type    text        NOT NULL,
    aggregate_id      text        NOT NULL,
    aggregate_version bigint      NOT NULL,
    occurred_at       timestamptz NOT NULL,
    effective_at      timestamptz,
    correlation_id    uuid        NOT NULL,
    causation_id      uuid,
    actor             jsonb       NOT NULL,
    traceparent       text,
    payload           bytea       NOT NULL,
    published_at      timestamptz
);

CREATE INDEX outbox_event_unpublished_idx ON outbox_event (occurred_at) WHERE published_at IS NULL;

CREATE TABLE inbox_message (
    consumer_name text        NOT NULL,
    event_id      uuid        NOT NULL,
    tenant_id     uuid        NOT NULL,
    processed_at  timestamptz NOT NULL,
    PRIMARY KEY (consumer_name, event_id)
);

CREATE TABLE idempotency_record (
    tenant_id       uuid        NOT NULL,
    principal_id    uuid        NOT NULL,
    operation       text        NOT NULL,
    idempotency_key text        NOT NULL,
    request_hash    text        NOT NULL,
    status          text        NOT NULL CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'FAILED_RETRYABLE')),
    response_status integer,
    response_body   bytea,
    created_at      timestamptz NOT NULL,
    expires_at      timestamptz, -- NULL = permanent (financial operations, CRC-IDEM-003)
    PRIMARY KEY (tenant_id, principal_id, operation, idempotency_key)
);

-- Tenant isolation. The application sets app.tenant_id per transaction from TenantContext.
-- In production the service connects as a non-owner role so RLS applies; the outbox relay uses a
-- dedicated role with BYPASSRLS.
ALTER TABLE outbox_event ENABLE ROW LEVEL SECURITY;
ALTER TABLE inbox_message ENABLE ROW LEVEL SECURITY;
ALTER TABLE idempotency_record ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON outbox_event
    USING (tenant_id = current_setting('app.tenant_id', true)::uuid);
CREATE POLICY tenant_isolation ON inbox_message
    USING (tenant_id = current_setting('app.tenant_id', true)::uuid);
CREATE POLICY tenant_isolation ON idempotency_record
    USING (tenant_id = current_setting('app.tenant_id', true)::uuid);
""",
    )

    # service
    write(
        base / f"{ctx}-service/src/main/java/{pkg_path}/service/{cls}ServiceApplication.java",
        f"""package {pkg}.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Deployable entry point of the {{@code {ctx}}} bounded context ({javadoc_prds(prds)}). */
@SpringBootApplication(scanBasePackages = "{pkg}")
public class {cls}ServiceApplication {{

  public static void main(String[] args) {{
    SpringApplication.run({cls}ServiceApplication.class, args);
  }}
}}
""",
    )
    write(
        base / f"{ctx}-service/src/main/resources/application.yml",
        f"""spring:
  application:
    name: {ctx}-service
  datasource:
    url: ${{DB_URL:jdbc:postgresql://localhost:5432/insurtech?currentSchema={ctx}}}
    username: ${{DB_USER:{ctx}_svc}}
    password: ${{DB_PASSWORD:{ctx}_svc}}
  flyway:
    schemas: {ctx}
    default-schema: {ctx}
    locations: classpath:db/migration/{ctx}
  kafka:
    bootstrap-servers: ${{KAFKA_BOOTSTRAP_SERVERS:localhost:9092}}

server:
  port: ${{SERVER_PORT:{a.port}}}

management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      probes:
        enabled: true
""",
    )
    write(
        base / f"{ctx}-service/src/test/java/{pkg_path}/service/{cls}ContractTest.java",
        f"""package {pkg}.service;

import com.insurtech.archtest.ContractRules;
import org.junit.jupiter.api.Test;

/** Fails the build when {{@code {pkg}}} violates the Code Rule Contract. */
class {cls}ContractTest {{

  @Test
  void obeysCodeRuleContract() {{
    ContractRules.verifyContext("{ctx}");
  }}
}}
""",
    )


# --------------------------------------------------------------------------------------------------
# BFF
# --------------------------------------------------------------------------------------------------


def scaffold_bff(a: argparse.Namespace) -> None:
    name = a.name
    if not re.fullmatch(r"[a-z][a-z0-9]*", name):
        sys.exit("BFF names are lowercase alphanumerics")
    cls = a.pascal or pascal(name)
    prds = [x.strip() for x in a.prds.split(",") if x.strip()]
    pkg = f"com.insurtech.bff.{name}"
    pkg_path = pkg.replace(".", "/")
    base = ROOT / "bff" / f"bff-{name}"
    print(f"Scaffolding BFF '{name}' at {base.relative_to(ROOT)}")

    write(
        base / "README.md",
        f"""# {a.title} BFF (`bff-{name}`)

| | |
|---|---|
| Owning PRDs | {prd_links(prds, 2)} |
| HTTP port (local) | `{a.port}` |
| Deployable | `:bff:bff-{name}` |

{a.summary}

A BFF composes bounded-context APIs for one client. It holds **no domain logic**: it never enforces business
rules, computes money, or decides state (CRC-ARCH-011). It may depend only on context `-api` modules and
platform libraries; the build and `{cls}BffContractTest` enforce this.
""",
    )
    write(
        base / "build.gradle.kts",
        'plugins {\n    id("insurtech.bff-conventions")\n}\n',
    )
    write(
        base / f"src/main/java/{pkg_path}/{cls}BffApplication.java",
        f"""package {pkg};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Backend-for-frontend: {a.title} ({javadoc_prds(prds)}). Composition only (CRC-ARCH-011). */
@SpringBootApplication
public class {cls}BffApplication {{

  public static void main(String[] args) {{
    SpringApplication.run({cls}BffApplication.class, args);
  }}
}}
""",
    )
    write(
        base / "src/main/resources/application.yml",
        f"""spring:
  application:
    name: bff-{name}

server:
  port: ${{SERVER_PORT:{a.port}}}

management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      probes:
        enabled: true
""",
    )
    write(
        base / f"src/test/java/{pkg_path}/{cls}BffContractTest.java",
        f"""package {pkg};

import com.insurtech.archtest.ContractRules;
import org.junit.jupiter.api.Test;

/** Fails the build when the BFF reaches into context internals (CRC-ARCH-011). */
class {cls}BffContractTest {{

  @Test
  void obeysCodeRuleContract() {{
    ContractRules.verifyBff("{name}");
  }}
}}
""",
    )


# --------------------------------------------------------------------------------------------------
# Local Postgres: one schema + one role per context (CRC-ARCH-005)
# --------------------------------------------------------------------------------------------------


def regenerate_postgres_init() -> None:
    contexts = sorted(p.name for p in (ROOT / "services").iterdir() if p.is_dir()) if (ROOT / "services").exists() else []
    lines = [
        "-- GENERATED by tools/scaffold_context.py. Do not edit by hand.",
        "-- Local development only: one schema and one login role per bounded context (CRC-ARCH-005).",
        "-- A role can use only its own schema; cross-context reads fail at the database (CRC-ARCH-004).",
        "",
    ]
    for ctx in contexts:
        lines += [
            f"CREATE ROLE {ctx}_svc LOGIN PASSWORD '{ctx}_svc';",
            f"CREATE SCHEMA {ctx} AUTHORIZATION {ctx}_svc;",
            "",
        ]
    target = ROOT / "deploy" / "postgres" / "init" / "01-context-schemas.sql"
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text("\n".join(lines), encoding="utf-8")
    print(f"  write  {target.relative_to(ROOT)} ({len(contexts)} contexts)")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = parser.add_subparsers(dest="kind", required=True)

    ctx = sub.add_parser("context", help="bounded-context microservice")
    ctx.add_argument("name")
    ctx.add_argument("--title", required=True)
    ctx.add_argument("--pascal", help="Java class prefix (default: derived from name)")
    ctx.add_argument("--aggregates", required=True, help="comma-separated; the first gets a typed ID")
    ctx.add_argument("--port", required=True, type=int)
    ctx.add_argument("--prds", required=True)
    ctx.add_argument("--tier", required=True, choices=["DEEP", "PARALLEL"])
    ctx.add_argument("--summary", required=True)

    bff = sub.add_parser("bff", help="backend-for-frontend")
    bff.add_argument("name")
    bff.add_argument("--title", required=True)
    bff.add_argument("--pascal")
    bff.add_argument("--port", required=True, type=int)
    bff.add_argument("--prds", required=True)
    bff.add_argument("--summary", required=True)

    args = parser.parse_args()
    if args.kind == "context":
        scaffold_context(args)
        regenerate_postgres_init()
    else:
        scaffold_bff(args)


if __name__ == "__main__":
    main()
