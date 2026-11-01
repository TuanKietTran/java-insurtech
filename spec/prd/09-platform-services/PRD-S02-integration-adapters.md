# PRD-S02 — Integration Adapters

| Field | Value |
|---|---|
| ID | PRD-S02 |
| Domain | Platform Services |
| Tier | **PARALLEL** |
| Status | Draft v0.1 |
| Owner | Integration Platform (TBD) |
| Depends on | F01 (partner auth, workload identity), F03 (events, idempotency), R01 (data minimization, DPAs), F02 |
| Consumed by | F04 (payment rails, bank files, GL export), B01 (PSP), U01/P03 (data enrichment), V01 (repair/IA networks, bill review), R03 (sanctions, fraud bureaus), X04 (email/SMS/push/print), F06 (e-signature), F07 (model providers via gateway), X02 (AMS/comparative raters), C01 (partner FNOL), R02 (submissions) |
| Key contract rules | CRC-ARCH-008; CRC-IDEM-005, 006; CRC-ERR-003, 004; CRC-SEC-005, 006, 011; CRC-TST-005, 006 |

## 1. Problem Statement

An insurance platform lives on external connections: payment processors and banks, motor-vehicle and property data, repair and estimating networks, medical bill review, sanctions lists, fraud bureaus, e-signature, print/mail, SMS and email, agency management systems, partner FNOL feeds, and the corporate GL. Point-to-point integrations written inside domain code leak vendor models into the domain, fail unpredictably, retry unsafely (double payments), and log personal data. The platform needs a consistent adapter framework — ports owned by domains, anti-corruption adapters per provider — with resilience, idempotency, observability, cost control, and simulators.

## 2. Goals
1. **Isolation**: 0 provider SDK/model types in `-domain` or `-application` modules (ArchUnit).
2. **Safe retries**: 0 duplicate external financial instructions caused by retries.
3. **Resilience**: provider outages degrade gracefully with no corrupted domain state; mean time to swap a provider for a port < 6 weeks.
4. **Testability**: 100% of adapters have simulators/sandboxes used in integration tests.
5. **Cost control**: paid data calls within per-tenant budgets with caching where licensed.

## 3. Non-Goals
- **General-purpose iPaaS** for customers' internal integrations.
- **Hosting partners' business logic.**
- **AI model integration** (owned by F07 gateway, which follows the same adapter rules).

## 4. Personas & User Stories
- As a **domain engineer**, I want to depend on a port (`SanctionsScreeningPort`, `PaymentRailPort`, `VehicleDataPort`) and get a tested adapter so that provider changes never touch my domain.
- As an **integration engineer**, I want a standard adapter skeleton with timeouts, retries, circuit breakers, idempotency, masking, metrics, and contract tests.
- As an **SRE**, I want a health dashboard per provider (latency, errors, breaker state, quota).
- As a **partner developer**, I want documented partner APIs (FNOL, quote, status webhooks) with sandbox credentials.
- As a **finance controller**, I want data-provider spend by tenant and use case.
- *Edge:* webhook from a PSP arrives twice or out of order → dedupe and order by provider sequence/timestamps.
- *Edge:* provider returns ambiguous result on a payment (timeout) → status query before any retry (CRC-IDEM-005).

## 5. Scope & Adapter Families
| Family | Examples of ports | Launch need |
|---|---|---|
| Payments | `PaymentRailPort` (ACH/wire/check print/card push), `PspPort` (tokenization, charges, refunds, webhooks), `BankStatementPort` (camt.053/BAI2), `PositivePayPort` | P0 |
| Finance | `GeneralLedgerExportPort` | P0 |
| Data enrichment | `VehicleDataPort` (VIN decode, symbols), `DriverHistoryPort` (MVR), `LossHistoryPort`, `PropertyDataPort`, `GeocodingPort`, `HazardDataPort`, `WeatherEventPort` | P0 (launch LOB subset) |
| Claims ecosystem | `RepairNetworkPort`, `EstimatingPlatformPort`, `IndependentAdjusterPort`, `MedicalBillReviewPort`, `SalvageAuctionPort`, `SubrogationArbitrationPort` | P0 (one each for launch LOB), P1 rest |
| Compliance | `SanctionsScreeningPort`, `FraudBureauPort`, `LicensingRegistryPort` | P0 sanctions; P1 others |
| Communications | `EmailPort`, `SmsPort`, `PushPort`, `PrintMailPort` | P0 |
| Documents | `ESignaturePort`, `MalwareScanPort`, `OcrPort` | P0 malware/OCR; P1 e-sign |
| Distribution | Partner APIs (FNOL, quote, status webhooks), `AgencyManagementPort` (ACORD), comparative rater intake | P0 partner FNOL API; P1 others |
| Identity | IdP federation (OIDC/SAML), SCIM | P0 |

## 6. Requirements
### P0
- **S02-P0-01 Port/adapter framework**: ports in consuming context's application layer; adapters in `integration-<family>-<provider>` modules; mapping (anti-corruption) to domain types.
- **S02-P0-02 Resilience defaults**: timeouts, bounded retries with jitter on idempotent ops, circuit breakers, bulkheads, fallback policies declared per port.
- **S02-P0-03 Idempotent outbound financial calls** with provider keys or status-query-before-retry.
- **S02-P0-04 Inbound webhooks**: signature verification, replay protection, dedupe (inbox), ordering, translation to domain commands/events.
- **S02-P0-05 Observability & masking**: request/response logging with field masking, OTel spans, per-provider metrics and health dashboard.
- **S02-P0-06 Simulators/sandboxes** for every adapter; contract tests against provider sandboxes in CI where available (CRC-TST-006).
- **S02-P0-07 Secrets & credentials** per tenant/provider in the secret manager; rotation support.
- **S02-P0-08 Cost & quota tracking** per tenant/use case with budgets and caching rules respecting data licenses.
- **S02-P0-09 Partner API gateway**: partner client registration, OAuth client credentials, rate limits, sandbox, OpenAPI docs, webhooks subscription for status events.
- **S02-P0-10 File-based integrations**: SFTP/object drop with checksum, schema validation, idempotent processing, reconciliation reports.
### P1
- **S02-P1-01** ACORD message mapping library (AL3/XML/JSON) for AMS and comparative raters.
- **S02-P1-02** Provider failover routing for critical ports (SMS, email, sanctions).
- **S02-P1-03** Self-service partner portal (keys, logs, webhook replays).
### P2
- **S02-P2-01** Marketplace of certified adapters per tenant.
- **S02-P2-02** Streaming integrations (telematics, IoT).

## 7. Interfaces & Events
- Each adapter family documents its port in the consuming context's `-api`/application module.
- **Published**: `integration.provider.degraded.v1`, `…recovered.v1`, `integration.quota.threshold-reached.v1`, `integration.webhook.rejected.v1`.

## 8. AI Integration Points
- Model providers are accessed exclusively via F07's gateway (CRC-AI-003), which reuses this framework's resilience, secrets, and masking.

## 9. Audit, Security & Compliance
- Data processing agreements tracked per provider (R01); outbound PII minimized to what the provider needs; third-party consumer-report usage logged with permissible purpose (U01).

## 10. Success Metrics
| Metric | Type | Target (hypothesis) |
|---|---|---|
| Provider types in domain/application modules | Leading | 0 |
| Duplicate external financial instructions | Lagging | 0 |
| Adapters with simulators | Leading | 100% |
| Provider-outage incidents causing state corruption | Lagging | 0 |
| Data spend within budget | Lagging | 100% of tenants |

## 11. Open Questions
- [Product] [Blocking] Launch provider list per family and market.
- [Legal] [Blocking] DPAs and data licensing terms (caching, retention) per data provider.

## 12. Dependencies & Phasing
- Framework and payments/communications/sanctions adapters first (gates for F04/X04/R03 go-live); enrichment and claims-ecosystem adapters in parallel per LOB launch plan.
