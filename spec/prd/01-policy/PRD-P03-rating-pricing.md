# PRD-P03 — Rating & Pricing Engine

| Field | Value |
|---|---|
| ID | PRD-P03 |
| Domain | Policy / Underwriting |
| Tier | **PARALLEL** (deterministic-output and worksheet obligations are non-negotiable) |
| Status | Draft v0.1 |
| Owner | Rating (TBD) |
| Depends on | P01 (rating variables, product versions), F05 (rule fragments), S02 (rating data lookups), F02 |
| Consumed by | P02 (quote, endorse, renew, cancel return premium), U01 (pricing deviations), X02 (comparative quoting), A01 |
| Key contract rules | CRC-MON-001…006; CRC-TIME-007; CRC-CFG-001…005; CRC-TST-007; CRC-AI-015 |

## 1. Problem Statement

Premiums must match filed rating plans exactly, be reproducible years later for regulators and disputes, and change on schedule when new rates take effect. Spreadsheet-derived or hard-coded rating logic drifts from filings, can't explain a premium step by step, and makes rate revisions an engineering project. The platform needs a configurable, deterministic rating engine that produces a full rating worksheet for every computation.

## 2. Goals

1. **Reproducibility**: re-rating any historical transaction with the same plan version and inputs yields the identical premium and worksheet (100% of golden cases).
2. **Explainability**: every premium has a step-level worksheet (factor lookups, intermediate values, rounding points) available to producers, underwriters, and regulators.
3. **Speed**: p95 < 300 ms per rating call for personal lines; batch re-rate of 1M renewals in < 4 hours.
4. **Agility**: rate revisions within existing plan structure are configuration-only, effective on separate NB/renewal dates.

## 3. Non-Goals

- **Actuarial rate development** (indications, GLM fitting). P03 executes approved plans.
- **Price optimization** beyond what is filed and permitted per jurisdiction.
- **Commission or billing fee calculation** (B02, B01), except premium-bearing fees/taxes defined in the rating plan.

## 4. Personas & User Stories

- As a **rating analyst**, I want to load a new rate table version (base rates, factor tables) with an effective date and validate it against filed exhibits so that implementation matches the filing.
- As a **producer**, I want to see why the premium changed between two quotes (factor deltas) so that I can explain it to the customer.
- As an **underwriter**, I want to apply schedule credits/debits within my authority and see their effect in the worksheet.
- As a **product actuary**, I want a rate-change impact simulation on the in-force book before the effective date (P1).
- As a **regulator liaison**, I want the worksheet for a specific policy transaction from two years ago.
- *Edge:* a required rating variable is missing → explicit error naming the variable, never a default factor.
- *Edge:* minimum premium applies after pro-rata → worksheet shows the floor step.

## 5. Domain Model & Invariants

| Entity | Notes |
|---|---|
| **Rating Plan** | per product; algorithm definition as ordered steps (lookup, multiply, add, cap/floor, round, allocate), referenced by product version |
| **Rating Plan Version** | effective NB/renewal dates per jurisdiction, content hash, filing reference, test cases |
| **Rate Table** | dimensions (e.g., territory × class × limit), values (`Factor`/`Money`), interpolation rules |
| **Rating Request** | product version, plan version (resolved by date or pinned), risk objects, coverages/options, term period, transaction type & effective date, prior premium (for endorsements) |
| **Rating Result** | premium by coverage and risk object, taxes/fees/surcharges, total, worksheet, warnings, plan version + hash |

**Invariants**
- INV-P03-1: Rating is pure and deterministic (no clock, no I/O during calculation; external data resolved before and passed in with provenance).
- INV-P03-2: Arithmetic per CRC-MON-001…004; rounding only at steps declared in the plan.
- INV-P03-3: Coverage-level premiums sum exactly to the total (largest-remainder allocation where splits occur).
- INV-P03-4: Published plan versions are immutable; the version used is stored with every result.

## 6. Requirements

### P0 — Must-have
- **P03-P0-01 Plan definition** as ordered, typed steps with rate tables; supports multiplicative/additive algorithms, caps/floors, minimum premium, per-coverage and policy-level steps.
- **P03-P0-02 Worksheet** for every result: each step's inputs, table key, value, intermediate result, rounding. *Acceptance:* worksheet totals reconcile to the premium to the minor unit.
- **P03-P0-03 Effective dating & resolution** (NB vs renewal by jurisdiction); pinned-version re-rate for historical reproduction.
- **P03-P0-04 Transaction premium methods**: full-term, pro-rata, short-rate, flat; endorsement deltas against prior version premium.
- **P03-P0-05 Taxes, fees, surcharges** by jurisdiction (configurable), separately itemized.
- **P03-P0-06 Golden-file suites** per plan version; publish blocked on failure (CRC-TST-007).
- **P03-P0-07 Rating data enrichment hooks**: territory/geocode, vehicle symbols, credit-based insurance score where permitted — resolved via S02 before rating with provenance stored.
- **P03-P0-08 UW adjustments**: schedule rating / IRPM credits and debits within authority (F01), recorded as worksheet steps.
- **P03-P0-09 Performance**: stateless horizontally scalable service; p95 < 300 ms personal lines.

### P1 — Fast follow
- **P03-P1-01** Batch re-rate and rate-change impact simulation on in-force/renewal books.
- **P03-P1-02** Quote-to-quote premium diff explanation API.
- **P03-P1-03** Import tooling from filed rate exhibits (spreadsheet → tables) with validation report.

### P2 — Future
- **P03-P2-01** Approved ML pricing components as governed factors (model version pinned and filed, fairness-tested).
- **P03-P2-02** Real-time usage-based rating.

## 7. Interfaces & Events
- `POST /v1/rating/rate` (sync), `POST /v1/rating/batch-jobs`; admin `/v1/rating-plans/{id}/versions…`.
- **Events**: `rating.rating-plan-version.published.v1`, `rating.batch-job.completed.v1`.

## 8. AI Integration Points
- None in the calculation path. P1 AI07 may explain worksheets in plain language from worksheet data (grounded, L2).

## 9. Audit, Security & Compliance
- Worksheet and plan version stored with every policy transaction (via P02); reproducible for exams and disputes.
- Prohibited rating factors per jurisdiction enforced at plan validation (CRC-AI-015 applies to rating variables).

## 10. Success Metrics

| Metric | Type | Target (hypothesis) |
|---|---|---|
| Golden reproduction success | Leading | 100% |
| Rating p95 | Leading | < 300 ms |
| Rate-revision implementation time | Lagging | ≤ 5 business days from approved exhibits |
| Premium disputes due to rating errors | Lagging | 0 |

## 11. Open Questions
- [Actuarial] [Blocking] Launch rating plan structures; any non-multiplicative algorithms required?
- [Legal] [Blocking] Permitted use of credit-based insurance scores and other consumer data per launch jurisdiction.
- [Engineering] [Non-blocking] Expression approach for steps: F05 decision tables vs. dedicated rating DSL.

## 12. Dependencies & Phasing
- Needs P01 rating-variable exposure and product version resolution. Can be developed in parallel with P02 against a stubbed rating contract; P02 quote flow integrates once P03-P0-01…06 exist.
