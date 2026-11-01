# PRD-A01 — Analytics & Operational Intelligence

| Field | Value |
|---|---|
| ID | PRD-A01 |
| Domain | Analytics & Intelligence |
| Tier | **PARALLEL** |
| Status | Draft v0.1 |
| Owner | Data Platform & Insights (TBD) |
| Depends on | F03 (event streams), CDC from context databases (read-only replicas), F01 (row-level security mapping), R01 (classification, masking), F02 (decisions), F04 (financial actuals) |
| Consumed by | Claims/UW/billing operations, finance, actuarial, executives, R02, F07 (online AI monitoring) |
| Key contract rules | CRC-ARCH-004 (consume via events/CDC only), CRC-EVT-*, CRC-SEC-006, CRC-TIME-003 |

## 1. Problem Statement

Claims leakage, cycle-time drift, fraud patterns, deteriorating loss ratios, and reserve inadequacy are discovered months late when metrics are assembled from inconsistent extracts with conflicting definitions ("which cycle time?"). Operators need near-real-time dashboards; finance and actuarial need reconciled, bitemporal history; AI governance needs override and accuracy trends. The platform needs a governed analytics layer fed by events/CDC, with a single semantic definition for each metric and tenant-safe access.

## 2. Goals
1. **One definition per metric**: 100% of dashboard metrics sourced from the versioned semantic layer.
2. **Freshness**: operational dashboards < 15 minutes latency; financial marts reconciled to ledger daily.
3. **Trust**: financial metrics (paid, incurred, written/earned premium) tie to F04 to the minor unit for closed periods.
4. **Adoption**: ≥ 80% of claims supervisors use operational dashboards weekly.
5. **Actionability**: leakage indicators drive measurable reduction (baseline then −15% leakage in QA samples within 12 months).

## 3. Non-Goals
- **Operational system of record** — analytics never writes back to domain state (CRC-ARCH-004).
- **Actuarial modeling tools** (consumers of the data).
- **Embedded BI inside every workbench at launch** (select embedded tiles only).

## 4. Personas & User Stories
- As a **claims supervisor**, I want inventory, aging, cycle time by segment, SLA breaches, workload, and reopen rates for my team.
- As a **claims VP**, I want leakage indicators (late reserve changes, missed subrogation/salvage, deductible misapplication, duplicate payment catches, vendor variance) by team and LOB.
- As a **CFO / actuary**, I want loss ratio (earned premium vs. incurred) by LOB/product/jurisdiction/producer, accident vs. calendar period, and case reserve development triangles.
- As an **SIU manager**, I want fraud pattern dashboards (signals, referrals, hit rates, savings).
- As a **UW manager**, I want quote-to-bind, referral rates and times, hit ratio, appetite drift.
- As a **billing manager**, I want DSO, delinquency roll rates, autopay penetration, refund timeliness.
- As an **AI governance officer**, I want per-AI-Agent acceptance, override, escalation, accuracy, cost per outcome.
- *Edge:* a user must only see their tenant's data (and their organization's scope where configured).

## 5. Domain Model (analytical)
- **Conformed dimensions**: tenant, organization, product/version, LOB, coverage, jurisdiction, producer, vendor, adjuster/team, segment, cause of loss, CAT event, date (accident, report, transaction, accounting), AI Agent/version.
- **Facts** (bitemporal): policy transactions & premium (written/earned), claims lifecycle transitions, claim financial movements, work items & SLAs, communications, vendor assignments, payments, recoveries, decisions & overrides, AI runs.
- **Semantic layer**: versioned metric definitions (owner, formula, grain, filters, caveats).

| Metric (examples) | Definition owner |
|---|---|
| Cycle time (FNOL → first payment; FNOL → close) per segment | Claims ops |
| Touch count / time to first contact | Claims ops |
| Reopen rate (30/90 days) | Claims ops |
| Leakage index (weighted indicators) | Claims quality |
| Loss ratio (AY/CY; paid and incurred) | Actuarial/finance |
| Initial reserve accuracy & case development | Actuarial |
| Subrogation identification & recovery rate | Recovery |
| SIU referral/acceptance/hit rates | SIU |
| Quote-to-bind, referral turnaround | Underwriting |
| DSO, delinquency roll rate | Billing |
| AI acceptance/override/escalation rate, cost per run/outcome | AI governance |

## 6. Requirements
### P0
- **A01-P0-01** Ingestion from F03 events and CDC into an analytical store with schema evolution handling; PII minimized/masked per R01 at ingestion.
- **A01-P0-02** Bitemporal modeling for policy, claim, and financial facts; as-of queries.
- **A01-P0-03** Semantic layer with versioned metric definitions and certification status.
- **A01-P0-04** Row-level security mapped from F01 (tenant, organization scope); tenant isolation tests.
- **A01-P0-05** Operational dashboards: claims ops, leakage, billing ops.
- **A01-P0-06** Financial marts reconciled daily to F04 (variance report).
- **A01-P0-07** Loss ratio and reserve development datasets (feeds R02).
- **A01-P0-08** AI performance datasets fed by F07/F02 (consumed by F07-P0-10 monitoring).
### P1
- **A01-P1-01** Self-service exploration for certified datasets.
- **A01-P1-02** Embedded tiles in workbenches (supervisor console, UW workbench).
- **A01-P1-03** Alerting on metric thresholds (e.g., cycle-time spike in a CAT region).
- **A01-P1-04** Fraud pattern analytics (feeds R03 tuning).
### P2
- **A01-P2-01** Predictive operational forecasting (claim volume, staffing).
- **A01-P2-02** Data sharing to capacity providers/reinsurers (governed extracts).

## 7. Interfaces & Events
- Consumes all integration events + CDC; exposes datasets/semantic API to BI tools; publishes `analytics.metric.threshold-breached.v1` (P1).

## 8. AI Integration Points
- P1: natural-language questions over certified metrics (AI, L2) with generated query shown and cited definitions; read-only.

## 9. Audit, Security & Compliance
- Access to analytics audited; sensitive classes excluded or masked; exports watermarked; retention aligned with R01.

## 10. Success Metrics
| Metric | Type | Target (hypothesis) |
|---|---|---|
| Dashboard metrics from semantic layer | Leading | 100% |
| Operational freshness | Leading | < 15 min |
| Ledger tie-out variance (closed periods) | Leading | 0 |
| Supervisor weekly active usage | Lagging | ≥ 80% |
| Leakage (QA sample) | Lagging | −15% at 12 months |

## 11. Open Questions
- [Data] [Blocking] Analytical store and transformation stack (lakehouse vs. warehouse) — ADR.
- [Finance] [Blocking] Loss ratio conventions (earned basis, ALAE inclusion) per tenant.
- [Claims] [Non-blocking] Leakage index weighting.

## 12. Dependencies & Phasing
- Starts as soon as F03 envelope and first event schemas exist; dashboards fill in as domains publish events. Fully parallel to domain work.
