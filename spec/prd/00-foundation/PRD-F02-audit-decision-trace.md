# PRD-F02 — Audit & Decision Trace

| Field | Value |
|---|---|
| ID | PRD-F02 |
| Domain | Foundation |
| Tier | **DEEP** |
| Status | Draft v0.1 |
| Owner | Platform Governance (TBD) |
| Depends on | F01 (actor identity), F03 (outbox delivery), R01 (crypto-shredding keys) |
| Consumed by | Every PRD; R02 (regulatory evidence); A01 (decision analytics) |
| Key contract rules | CRC-AUD-001…006; CRC-PER-007; CRC-SM-006; CRC-AI-004 |

## 1. Problem Statement

Insurers must explain to regulators, courts, reinsurers, and customers why a claim was paid or denied, why a premium is what it is, and who changed what when. Today's systems typically scatter this across application logs, note fields, and database history tables that can be edited and that do not capture *why* — especially not for AI-assisted work. Without an immutable, queryable trace of every state change and decision, the platform cannot meet its core promise: operators can always see why a decision was made, human or AI.

## 2. Goals

1. **100% of state-changing commands** produce an audit record in the same transaction (verified by harness and by reconciliation of events vs. audit).
2. **100% of business decisions** (rule, AI, human-with-authority) are captured as Decision Records with versions and explanation.
3. **Tamper evidence**: any modification of historical audit data is detected by the integrity verifier within 24 hours.
4. **Answer "why" in under 2 seconds** for any claim, policy, or payment timeline (p95).
5. Regulator/auditor evidence packs for a claim or policy can be produced self-service in under 5 minutes.

## 3. Non-Goals

- **Operational logging/APM.** Logs remain separate (CRC-AUD-005).
- **Business-user note-taking.** Claim notes and diaries live in C03; they are audited, but they are not the audit trail.
- **SIEM.** Security events are exported to the tenant's SIEM; F02 is not a SIEM.
- **Blockchain anchoring.** Conventional append-only storage with hash chaining and WORM retention is sufficient.

## 4. Personas & User Stories

**Claims supervisor / quality auditor**
- As a claims QA auditor, I want a single chronological timeline of everything that happened on a claim (state changes, reserve moves, payments, coverage decisions, AI recommendations, overrides, document access) so that I can review handling quality.
- As a supervisor, I want to filter a claim timeline to "decisions where a human overrode an AI or rule" so that I can coach and calibrate.

**Compliance officer / regulator liaison**
- As a compliance officer, I want to export an evidence pack (timeline, decisions, letters sent, documents) for a market-conduct exam so that I can respond within the regulator's deadline.
- As a compliance officer, I want to reconstruct what the system knew at a past date (point-in-time) so that I can defend decisions made with the information available then.

**Data protection officer**
- As a DPO, I want erasure requests honored without breaking audit immutability so that we comply with both privacy and record-keeping law.

**Engineer / SRE**
- As an engineer, I want to trace an AI recommendation from the UI back to model version, prompt version, retrieved documents, and tool calls so that I can debug a bad suggestion.

**Edge cases**
- As an auditor, I need to see that a record was *read* (PHI viewed) even if no change occurred.
- As an auditor, if the integrity check fails, I need to know which segment is affected and since when.

## 5. Domain Model & Invariants

**AuditRecord** (append-only)
| Field | Notes |
|---|---|
| auditId (ULID), tenantId | |
| actor | `ActorRef`: type (`HUMAN`/`SYSTEM`/`AI_AGENT`/`EXTERNAL`), principalId, on-behalf-of chain, organization |
| action | command type (e.g., `ApproveClaimPayment`) or `READ_SENSITIVE` |
| target | aggregateType, aggregateId, versionBefore, versionAfter |
| changeSet | structured diff of business fields (sensitive values tokenized) |
| reason | reason code (configurable taxonomy) + free text where required |
| authority | authority check result: limit used, amount, outcome |
| context | correlationId, causationId, channel, client/device metadata (classified), idempotencyKey |
| time | recordedAt (Instant), effectiveAt (business time) |
| integrity | sequence number within partition, `prevHash`, `hash` |

**DecisionRecord** (append-only, linked to AuditRecord(s))
| Field | Notes |
|---|---|
| decisionId, decisionType | e.g., `COVERAGE_POSITION`, `TRIAGE_SEGMENT`, `RESERVE_RECOMMENDATION`, `UW_REFERRAL`, `FRAUD_SCORE` |
| subject | aggregate references |
| source | `RULE` (ruleSetId+version+hash), `AI` (aiAgentId+version, model, prompt version, runId), `HUMAN` (principal, authority) |
| inputs | references + content hash of input snapshot (coverage snapshot ID, facts hash, retrieved doc spans) |
| output | structured outcome |
| explanation | fired rules / reason codes / citations / feature attributions |
| confidence | for AI and scores |
| relation | `recommends` / `supersedes` / `overrides` another decisionId |
| override | reason code + justification (mandatory when overriding) |

**Invariants**
- INV-F02-1: An aggregate version exists only if an AuditRecord for that transition exists (enforced by same-transaction write via outbox).
- INV-F02-2: Records are never updated or deleted by application roles (CRC-PER-007).
- INV-F02-3: Per partition (tenant × day × shard), `hash = H(prevHash ‖ canonical(record))`; chain heads are periodically sealed to WORM storage.
- INV-F02-4: A human decision differing from the most recent applicable recommendation MUST carry an override relation and reason.
- INV-F02-5: Personal data inside audit is stored tokenized or encrypted under the data subject's key; erasure destroys the key (crypto-shredding) without altering hashes of ciphertext.

## 6. Requirements

### P0 — Must-have

**F02-P0-01 Audit port & guaranteed capture.** `AuditPort.record(...)` used by every command handler; written via outbox in the command's transaction. A reconciliation job verifies every published domain event has a matching audit record.
*Acceptance:* Given a command handler that changes state, when its transaction commits, then exactly one AuditRecord exists referencing the aggregate's new version; a handler without audit fails the command-handler test harness (CRC-AUD-001).

**F02-P0-02 Decision records.** Rules engine (F05) and AI harness (F07) emit DecisionRecords automatically; human decisions under authority (coverage position, reserve change, payment approval, UW decision) emit them via application services.
*Acceptance:* for every claim payment, the timeline shows the decision chain: recommendation(s) → approval decision(s) → ledger posting reference.

**F02-P0-03 Override capture.** UI and APIs require reason code + justification whenever a human outcome differs from a rule/AI recommendation; the override is linked to the recommendation.

**F02-P0-04 Immutable, tamper-evident storage.** Append-only tables with deny-update/delete grants, hash chaining, daily sealing of chain heads to WORM object storage, and an integrity verifier.
*Acceptance:* manual alteration of any record in a test environment is reported by the verifier with partition and sequence range.

**F02-P0-05 Query API & timelines.** Query by aggregate (full timeline with related aggregates: claim → claim lines → payments → documents), by actor, by decision type, by time range, by correlation ID. p95 < 2 s for a claim with up to 5,000 records.

**F02-P0-06 "Why" view.** For any decision: inputs, versions, explanation, confidence, actor, approvals, overrides, and downstream effects, rendered for operators (consumed by X-portals and internal workbenches).

**F02-P0-07 Point-in-time reconstruction.** Reconstruct the state of an aggregate and the configuration versions in effect as known at a given transaction time.

**F02-P0-08 Sensitive read audit.** Reads of `SENSITIVE_PII`, `PHI`, `FINANCIAL_ACCOUNT` fields and document views/downloads are recorded (CRC-SEC-012).

**F02-P0-09 Retention & legal hold.** Retention classes by record type and jurisdiction (R01); legal holds suspend expiry; expiry is performed only by the retention service with its own audit record.

**F02-P0-10 Crypto-shredding compatibility.** PII in change sets is tokenized/encrypted per data subject so erasure (R01) does not break the chain.

**F02-P0-11 Evidence pack export.** Export of timeline + decisions + referenced documents + communications for a claim/policy as a signed bundle (PDF summary + machine-readable JSON + manifest with hashes).

### P1 — Fast follow
- **F02-P1-01** Override analytics feed to A01 (override rate by decision type, AI Agent, user, rule).
- **F02-P1-02** Anomaly alerts: unusual override bursts, after-hours large-authority use, repeated break-glass.
- **F02-P1-03** Auditor role with read-only, time-boxed, scoped access and watermarking of exports.
- **F02-P1-04** Diff viewer between two versions of any aggregate.

### P2 — Future
- **F02-P2-01** External notarization of chain heads (e.g., RFC 3161 timestamping authority).
- **F02-P2-02** Regulator self-service portal with scoped evidence access.

## 7. Interfaces & Events
- `AuditPort.record(AuditRecord)`; `DecisionPort.record(DecisionRecord)`; query API `/v1/audit/timeline?aggregateType=&aggregateId=`, `/v1/decisions/{id}`, `/v1/decisions?subject=&type=`.
- **Events published**: `audit.integrity.verified.v1`, `audit.integrity.violation-detected.v1`, `audit.evidence-pack.generated.v1`.
- **Consumes**: all domain events (for reconciliation); `privacy.subject-key.destroyed.v1` (R01).

## 8. AI Integration Points
- Every AI run produces a DecisionRecord linked to its run trace (F07).
- P1: AI-generated timeline summaries for supervisors (AI06 drafting, L2), always linking to underlying records.

## 9. Audit, Security & Compliance
- Access to audit data is itself audited.
- Supports market-conduct exams, unfair-claims-practices defense, SOC 2, and model-risk-governance evidence for AI.

## 10. Success Metrics

| Metric | Type | Target (hypothesis) |
|---|---|---|
| Commands without audit record | Leading | 0 (reconciliation job) |
| Decisions without DecisionRecord (sampled QA) | Leading | 0 |
| Integrity verification failures | Lagging | 0 unexplained |
| Timeline query p95 | Leading | < 2 s |
| Evidence pack time-to-produce | Lagging | < 5 min self-service |

## 11. Open Questions
- [Engineering] [Blocking] Storage: Postgres partitions + object storage archive vs. dedicated append-only store; decide by ADR with volume estimates.
- [Legal] [Blocking] Retention periods per record class and jurisdiction (input from R01).
- [Compliance] [Non-blocking] Which read events are mandatory vs. sampled for non-sensitive data?

## 12. Dependencies & Phasing
- **First**: AuditPort, schema, outbox delivery, append-only storage — must exist before any DEEP command handler ships.
- **Then**: DecisionRecord, override capture, query API, integrity verifier.
- **Parallel**: evidence pack UI, analytics feed, anomaly alerts.
