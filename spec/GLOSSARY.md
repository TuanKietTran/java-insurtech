# Glossary — Ubiquitous Language

Class names, API fields, event names, UI labels, and PRDs MUST use these terms (CRC-DOM-011). When a term is ambiguous in the industry, this file picks one meaning for the platform.

> **Two kinds of "agent".** The platform never uses the bare word "agent".
> - **Producer** — a licensed insurance agent or broker (person or agency) who sells and services policies.
> - **AI Agent** — an LLM-based agent executed by the AI Agent Harness (F07).

## Tenancy & identity

| Term | Definition |
|---|---|
| **Tenant** | Top-level isolation boundary: a carrier, MGA, or program administrator operating on the platform. All data belongs to exactly one tenant. |
| **Organization** | A legal or operational entity inside a tenant: carrier unit, producer agency, vendor firm, TPA. Organizations form hierarchies. |
| **Principal** | Any authenticated actor: human user, service account, external API client, or AI Agent. |
| **Actor type** | `HUMAN`, `SYSTEM`, `AI_AGENT`, `EXTERNAL`. Recorded on every audit record. |
| **On-behalf-of** | Delegation chain: e.g., AI Agent acting for an adjuster; a CSR acting for an insured. |
| **Authority Limit** | Maximum amount or decision class a principal may approve (reserve, payment, write-off, UW pricing deviation, binding). |
| **PDP** | Policy Decision Point: the authorization service that evaluates RBAC + ABAC policies. |

## Product & policy

| Term | Definition |
|---|---|
| **Line of Business (LOB)** | Category of insurance: personal auto, homeowners, commercial property, general liability, specialty, etc. |
| **Product** | A sellable insurance offering for one or more LOBs in a set of jurisdictions. |
| **Product Version** | Immutable, effective-dated snapshot of a product's configuration (coverages, forms, rules, rating plan). |
| **Package** | A product combining multiple LOBs/coverage parts in one policy (e.g., a business owner's policy). |
| **Coverage** | A promise to pay for a defined type of loss (e.g., Collision, Dwelling, Bodily Injury Liability). Identified by a configurable coverage code. |
| **Limit** | Maximum amount payable: per occurrence, per person, per claim, aggregate, or sublimit. |
| **Deductible / Retention** | Amount borne by the insured: flat, percentage, franchise, or per-occurrence/aggregate. |
| **Exclusion** | Condition under which a coverage does not apply. |
| **Condition** | Obligation of the parties (e.g., timely notice, cooperation). |
| **Form** | A versioned wording document (form number + edition date) that defines coverage language. |
| **Endorsement (form)** | A form that adds, removes, or modifies coverage. "Rider" is a synonym used in some lines. |
| **Risk Object** | The insured thing or exposure: vehicle, driver, building, location, operations, schedule item. Shape defined by a product schema. |
| **Quote** | A priced, not-yet-bound offer for a policy term or transaction. |
| **Bind** | Commitment to provide coverage; coverage attaches at the effective date even before issuance. |
| **Issue** | Production of the official policy documents and policy number for a bound term. |
| **Policy** | Durable identity of a contract across renewals (policy number). |
| **Policy Term** | One contract period (e.g., 12 months) of a policy, with its own lifecycle. |
| **Policy Transaction** | A change to a term: New Business, Endorsement, Cancellation, Reinstatement, Renewal, Rewrite. |
| **Policy Version** | Immutable result of applying a transaction; valid over an effective-date interval and recorded at a transaction time. |
| **Effective date / time** | When a fact is true in the business world (valid time). |
| **Transaction time / recorded-at** | When the platform learned the fact (system time). Together with effective date this forms the **bitemporal** model. |
| **OOSE** | Out-of-sequence endorsement: an endorsement whose effective date precedes an already-applied later transaction. |
| **Coverage Snapshot** | Immutable view of the coverages, limits, deductibles, forms, and risk objects in force on a policy at a specific effective instant, as known at a specific transaction time. The contract between Policy and Claims. |
| **Cancellation method** | Flat (from inception), pro-rata, or short-rate. |
| **Reinstatement** | Restoring a cancelled term, with or without a lapse in coverage. |
| **Written / Earned / Unearned premium** | Premium booked by transactions / recognized over elapsed coverage / not yet recognized. |

## Claims

| Term | Definition |
|---|---|
| **Loss** | The event causing damage or injury (date/time, location, cause). |
| **Loss Report** | Raw FNOL submission before it becomes a claim (C01). |
| **FNOL** | First Notice of Loss. |
| **Claim** | The platform's record for a reported loss under one policy. |
| **Claim Line** | One claimant × one coverage within a claim (industry synonyms: feature, exposure). Reserves, payments, coverage positions, and closure are held at this level. |
| **Party** | A person or organization involved in a claim: insured, claimant, third party, witness, attorney, vendor, lienholder/mortgagee. |
| **Claimant** | A party asserting a right to payment on a claim line. |
| **Coverage Position** | The determination for a claim line: `PENDING`, `COVERED`, `PARTIALLY_COVERED`, `NOT_COVERED`, `RESERVATION_OF_RIGHTS`. |
| **Reservation of Rights (ROR)** | Notice that the insurer is handling a claim while reserving the right to deny coverage. |
| **Liability Assessment** | Determination of fault percentage among parties for a third-party claim line. |
| **Reserve** | Estimate of the future amount payable, held per claim line × cost category. |
| **Cost Category** | Indemnity (loss), Expense — ALAE/DCC (defense & cost containment), A&O (adjusting & other), Medical, etc. Configurable. |
| **Paid / Outstanding / Incurred** | Paid to date / remaining reserve / Paid + Outstanding − Recoveries (net incurred). |
| **Settlement** | Agreement on the amount to resolve a claim line. |
| **Payment Order** | Instruction to move money out (claim payment, refund, commission, vendor payment) through F04. |
| **Recovery** | Money recovered: subrogation, salvage, deductible recovery, overpayment recovery, reinsurance (P2). |
| **Subrogation** | Pursuing a responsible third party (or their insurer) to recover amounts paid. |
| **Salvage** | Proceeds from disposing of damaged property (e.g., a total-loss vehicle). |
| **Triage** | Classifying a claim's complexity, severity, and handling segment. |
| **Segment** | Handling track: `STRAIGHT_THROUGH`, `FAST_TRACK`, `STANDARD`, `COMPLEX`, `LARGE_LOSS`, `SIU`. Configurable. |
| **Work Item** | A task, diary entry, approval, or review assigned to a principal or queue, with an SLA. |
| **Diary** | Scheduled follow-up on a claim. |
| **SIU** | Special Investigations Unit (fraud). |
| **Litigation flag** | Claim (line) is in suit or the claimant is attorney-represented. |
| **CAT event** | Catastrophe event code grouping claims from one event (e.g., a named storm). |
| **Leakage** | Money paid that should not have been (overpayment, missed recovery, process failure). |

## Underwriting & billing

| Term | Definition |
|---|---|
| **Submission** | An application for coverage received for underwriting. |
| **Clearance** | Checking whether a submission duplicates one already received (often from another producer). |
| **Referral** | Routing a quote/submission to an underwriter because a rule or score requires human review. |
| **Subjectivity** | A condition that must be satisfied after binding (e.g., signed application). |
| **Rating Worksheet** | Step-by-step record of how a premium was computed. |
| **Billing Account** | The account that owes premium (direct bill) or remits it (agency bill). |
| **Billing Plan** | Payment schedule: pay-in-full, monthly, quarterly, etc. |
| **Invoice** | Statement of amounts due by a date. |
| **Delinquency** | Overdue balance past grace period, leading to notice of intent to cancel. |
| **Write-off** | Recognizing an uncollectible or immaterial balance. |
| **Commission** | Producer compensation, typically a percentage of written or collected premium. |
| **Chargeback (commission)** | Recovery of unearned commission after cancellation or return premium. |

## Ledger

| Term | Definition |
|---|---|
| **Journal Entry** | Balanced set of debit and credit lines recorded atomically. |
| **Posting** | Writing a journal entry. Postings are append-only. |
| **Posting Rule** | Versioned mapping from a business event to a journal-entry template. |
| **Account** | Ledger account in the tenant's chart of accounts. |
| **Dimensions** | Analytical tags on journal lines: policy, term, claim, claim line, coverage, LOB, jurisdiction, producer, vendor, payee. |
| **Reversal** | A new entry that negates a prior entry; the only way to correct a posting. |
| **Accounting Period** | Calendar period that can be open or closed for postings. |
| **Sub-ledger** | Platform ledger detail that summarizes into the external general ledger (ERP). |

## AI & decisions

| Term | Definition |
|---|---|
| **AI Agent Definition** | Versioned configuration of an AI Agent: purpose, model, prompt version, tools, budgets, autonomy level, eval suite. |
| **Tool** | A typed, permissioned function an AI Agent may call. Side-effect class: `READ`, `PROPOSE`, `MUTATE`. |
| **Run** | One execution of an AI Agent, fully traced. |
| **Recommendation** | AI output suggesting a value or decision for a human or rule to act on. |
| **Proposal** | A pending change produced by a `PROPOSE` tool, awaiting an approval gate. |
| **Approval Gate** | Human (or configured authority) step that accepts, edits, or rejects a Proposal. |
| **Autonomy Level** | L0 Off · L1 Shadow · L2 Assist · L3 Act-with-approval · L4 Bounded autonomous (F07). |
| **Decision** | Any outcome the business relies on: rule evaluation, AI recommendation, human determination. |
| **Decision Record** | Immutable record of a decision: inputs, versions, outputs, explanation, actor, confidence, override. |
| **Override** | A human decision that differs from a rule or AI recommendation; requires a reason. |
| **Eval Suite** | Versioned dataset + metrics + thresholds used to gate AI changes. |
| **Grounded** | An AI assertion backed by a cited source (document span or record ID). |
