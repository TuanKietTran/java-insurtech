-- Platform tables every service owns in its own schema (search).
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
