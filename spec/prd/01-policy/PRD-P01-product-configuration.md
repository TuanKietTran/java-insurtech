# PRD-P01 — Product Configuration & Coverage Model

| Field | Value |
|---|---|
| ID | PRD-P01 |
| Domain | Policy |
| Tier | **DEEP** |
| Status | Draft v0.1 |
| Owner | Product Platform (TBD) |
| Depends on | F05 (rules), F06 (forms/templates), F02, F03 |
| Consumed by | P02, P03, U01, C01, C03, C04, C05, B01, B02, X01–X03, AI03, AI07, S01 |
| Key contract rules | CRC-CFG-001…005; CRC-DOM-005, 012; CRC-TIME-003, 004; CRC-TST-007 |

## 1. Problem Statement

Every new product, state filing, or coverage tweak in legacy insurance systems tends to require code changes across quoting, policy, billing, and claims, so launches take quarters and the coverage logic claims uses drifts from what was sold. The platform needs a single, versioned, effective-dated product model — coverages, limits, deductibles, exclusions, endorsements, forms, risk-object shapes, and the rules that govern them — that every downstream context reads, so that new products and lines of business are mostly configuration and coverage decisions trace back to the exact product version sold.

## 2. Goals

1. **Configuration over code**: a new product built from supported coverage patterns goes from approved design to quotable in test with **zero code deployments** ("New LOB drill" passes).
2. **Single source of coverage truth**: 100% of coverage determinations (C04) reference the product version and form editions on the policy.
3. **Safe change**: every product version publishes with passing validation and scenario tests; zero production incidents from structurally invalid product configuration.
4. **Time to launch a rate/form revision** in an existing product: ≤ 2 weeks from approved filing to effective.

## 3. Non-Goals

- **Rate computation.** Rating plans live in P03 (referenced by product version).
- **Regulatory filing preparation and submission** (SERFF-style). P01 stores filing references and status; filing authoring is external (P2 integration).
- **Marketing/product catalog content** for websites.
- **Arbitrary new coverage *patterns*** (e.g., parametric triggers) without engineering. New patterns are platform capabilities added deliberately; products compose existing patterns.

## 4. Personas & User Stories

**Product manager / product actuary**
- As a product manager, I want to define a product version with its coverages, limit and deductible options, eligibility rules, forms, and rating plan reference so that the product is quotable without engineering.
- As a product manager, I want to clone the current version, change a deductible option for one jurisdiction, and see a diff so that revisions are low-risk.
- As a product manager, I want distinct effective dates for new business and renewals so that rate/form changes follow filing rules.

**Product compliance analyst**
- As a compliance analyst, I want each form edition and rule tied to a filing reference and approval status per jurisdiction so that we never sell unfiled forms.

**Claims configuration analyst**
- As a claims configuration analyst, I want to define per-coverage claim settings (claim line types, cost categories, required documents, fast-track eligibility, coverage rule set) so that claims handling follows product design.

**Engineer**
- As a downstream engineer, I want a typed product-definition read API resolved by (product, jurisdiction, channel, effective date) so that I never hard-code product logic (CRC-CFG-002).

**Edge cases**
- A risk-object schema change (e.g., add "roof age") must not invalidate in-force policies created with the earlier schema.
- A jurisdiction is withdrawn from a product: new business blocked from a date; renewals handled per configured withdrawal rules.

## 5. Domain Model & Invariants

```
Product ─┬─ ProductVersion (effective: NB from / renewal from / to; jurisdictions; status)
         │    ├─ LineOfBusiness / CoveragePart[]
         │    │    └─ CoverageDefinition[]
         │    │         ├─ code, name, type (FIRST_PARTY | THIRD_PARTY), trigger (OCCURRENCE | CLAIMS_MADE | …)
         │    │         ├─ LimitStructure (per occurrence / per person / per claim / aggregate / sublimits; options)
         │    │         ├─ DeductibleStructure (flat / % of value / franchise / waiting period; options)
         │    │         ├─ applicable RiskObject types; required/optional/default; dependencies & exclusivity
         │    │         ├─ covered perils/causes of loss; exclusions; conditions (with form clause refs)
         │    │         └─ ClaimSettings (claim line types, cost categories, coverage rule set, required docs)
         │    ├─ EndorsementDefinition[] (form ref, effect: add/modify/remove coverage or attribute, eligibility)
         │    ├─ FormSchedule (form number + edition, applicability by jurisdiction/coverage, filing refs)
         │    ├─ RiskObjectSchema[] (JSON Schema per type: vehicle, driver, building, location, operation, item)
         │    ├─ Rule references (eligibility, validation, UW referral, cancellation/notice, renewal)
         │    ├─ RatingPlan reference (P03), BillingPlans allowed (B01), CommissionPlan refs (B02)
         │    └─ TermSettings (term length options, effective-time convention & zone basis, cancellation methods)
         └─ Package composition (multi-LOB products referencing coverage parts)
```

**Product Version lifecycle**: `DRAFT → IN_REVIEW → APPROVED → PUBLISHED → (CLOSED_TO_NEW_BUSINESS) → RETIRED` (+ `REJECTED`). Published content is immutable.

**Invariants**
- INV-P01-1: Published product versions, coverage definitions, and form schedules are immutable; changes create new versions.
- INV-P01-2: A policy version references exactly one product version; that reference never changes for the life of that policy version.
- INV-P01-3: A product version can publish only if all referenced artifacts (forms, rule set versions, rating plan version, schemas, templates) exist, are approved, and are effective for its period.
- INV-P01-4: For a (product, jurisdiction, channel, date) at most one version is effective for sale.
- INV-P01-5: Coverage, peril, cause-of-loss, cost-category codes are config-backed value types (CRC-DOM-005), unique within a tenant's code registry, and never repurposed.
- INV-P01-6: Risk-object schemas evolve compatibly within a product (additive); breaking changes require a new product version with migration rules for renewals.

## 6. Requirements

### P0 — Must-have

**P01-P0-01 Product & version model** as above, supporting personal auto, homeowners/property, general liability, and one specialty pattern at launch, including multi-LOB packages.
*Acceptance:* the launch product set is expressible without code beyond supported patterns; the "New LOB drill" (configure a new product from patterns, quote, bind, issue, register and determine a claim in test) passes with zero deployments.

**P01-P0-02 Coverage patterns library.** Platform-supported limit, deductible, trigger, and coinsurance patterns with clear semantics consumed by P03 (rating) and C04 (coverage determination).

**P01-P0-03 Risk-object schemas.** JSON Schema per risk-object type with validation rules, UI hints, data-enrichment hooks (S02), PII classification per field (R01).

**P01-P0-04 Forms & editions.** Form registry with number, edition date, jurisdiction applicability, filing reference/status, linked F06 document version; form attachment rules by coverage/endorsement/jurisdiction.

**P01-P0-05 Endorsement definitions.** Endorsements that add/modify/remove coverages or attributes, with eligibility rules and form linkage; usable at new business and mid-term.

**P01-P0-06 Effective dating.** NB and renewal effective dates per jurisdiction; scheduled publication; close-to-new-business; resolution API by (product, jurisdiction, channel, date).

**P01-P0-07 Claims settings per coverage.** Claim line types, cost categories, coverage rule set, liability rule set (third-party), required documents, triage/fast-track rule refs, reserve defaults (C05).

**P01-P0-08 Validation & scenario testing.** Structural validation (completeness, references, option consistency); bundled scenario tests (quote/bind/issue/claim fixtures) executed before publish (CRC-TST-007).

**P01-P0-09 Governance.** Draft → review → approve → publish with four-eyes and role separation (product vs. compliance); diff between versions; audit.

**P01-P0-10 Config-as-code.** Export/import product versions as versioned files (Git-backed), enabling code review and environment promotion; UI and files stay consistent.

**P01-P0-11 Read API.** Typed, cached product-definition API with ETags; coverage code registry API.

### P1 — Fast follow
- **P01-P1-01** Visual product designer UI with coverage pattern palette.
- **P01-P1-02** Impact analysis: which in-force policies/renewals are affected by a new version.
- **P01-P1-03** Product templates (start new products from a base product).
- **P01-P1-04** Jurisdiction withdrawal workflows with renewal treatment rules.

### P2 — Future
- **P01-P2-01** Filing system integration (export filing packages, import approvals).
- **P01-P2-02** New coverage patterns: parametric triggers, usage-based exposure, embedded micro-products.
- **P01-P2-03** Tenant-shared product libraries (MGA programs across carriers).

## 7. Interfaces & Events
- `GET /v1/products/{id}/versions/resolve?jurisdiction=&channel=&date=`; `GET /v1/product-versions/{id}`; admin lifecycle commands `…/submit|approve|publish|close|retire`.
- **Events published**: `product.product-version.published.v1`, `…closed-to-new-business.v1`, `…retired.v1`, `product.form.edition-published.v1`.

## 8. AI Integration Points
- AI07 uses form wording (via S01) tied to product versions for plain-language explanations.
- P2: AI-assisted product drafting from filed rule manuals (L2 assist; humans author and approve).

## 9. Audit, Security & Compliance
- Every product change audited; product version + form editions recorded on every policy version and decision.
- Supports regulator review: "show the forms and rules in effect for this policy on this date."
- Fairness: eligibility rules cannot reference protected attributes (CRC-AI-015 applied to rules).

## 10. Success Metrics

| Metric | Type | Target (hypothesis) |
|---|---|---|
| Code deploys required for new pattern-based product | Leading | 0 |
| Time from approved filing to effective (revision) | Lagging | ≤ 2 weeks |
| Invalid product versions reaching production | Lagging | 0 |
| Coverage decisions referencing product version + forms | Leading | 100% |

## 11. Open Questions
- [Product] [Blocking] Launch LOBs and jurisdictions (drives pattern library scope).
- [Actuarial] [Blocking] Which rating constructs must P01 expose to P03 (rating variables per risk object).
- [Legal] [Non-blocking] Filing reference requirements per jurisdiction; treatment of use-and-file vs. prior approval.

## 12. Dependencies & Phasing
- **First**: coverage model, risk-object schemas, forms registry, effective-dated resolution, claims settings — P02, P03, C04 build against this.
- **Then**: governance workflow, scenario test harness, config-as-code.
- **Parallel**: visual designer, impact analysis, templates.
