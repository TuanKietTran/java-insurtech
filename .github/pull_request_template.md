## Summary

<!-- What changes and why. -->

### Traceability
- PRD requirement IDs:
- ADRs (if any):

### Contract checklist (spec/CODE-RULE-CONTRACT.md)
- [ ] New/changed commands: authorized (CRC-SEC-001), audited (CRC-AUD-001), idempotent (CRC-IDEM-001)
- [ ] Lifecycle changes: transition table + tests + regenerated diagram (CRC-SM-001/005)
- [ ] Money: Money type, explicit rounding, exact allocation (CRC-MON-*)
- [ ] Time: injected Clock, explicit effective dates (CRC-TIME-002/007)
- [ ] Events: outbox, envelope, schema compatibility (CRC-EVT-*)
- [ ] PII: classified, encrypted/masked, not in telemetry (CRC-PER-009, CRC-SEC-006)
- [ ] Config over code: no jurisdiction/LOB branches (CRC-CFG-002)
- [ ] AI: gateway only, prompt versioned, evals passed, denylist respected (CRC-AI-*)

### Verification
<!-- Commands run and results, e.g. ./gradlew build -->
