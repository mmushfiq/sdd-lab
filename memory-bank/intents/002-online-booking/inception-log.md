---
intent: 002-online-booking
created: 2026-10-29T07:58:01Z
completed: 2026-10-29T07:58:01Z
status: complete
---

# Inception Log: 002-online-booking

## Overview

**Intent**: Guests search, book, view and cancel online (public /booking area).
**Type**: green-field
**Created**: 2026-10-29T07:58:01Z
**Source**: `docs/hotel-requirements.md`

## Artifacts Created

| Artifact | Status | File |
|----------|--------|------|
| Requirements | ✅ | requirements.md |
| System Context | ✅ | system-context.md |
| Units | ✅ | units.md, units/*/unit-brief.md |
| Stories | ✅ | units/*/stories/*.md (4) |
| Bolt Plan | ✅ | memory-bank/bolts/004-guest-booking |

## Summary

| Metric | Count |
|--------|-------|
| Functional Requirements | 5 |
| Units | 1 |
| Stories | 4 |
| Bolts Planned | 1 |

## Units Breakdown

| Unit | Stories | Bolts | Priority |
|------|---------|-------|----------|
| 001-guest-booking | 4 | 1 | Must |

## Decision Log

| Date | Decision | Rationale | Approved |
|------|----------|-----------|----------|
| 2026-10-29T07:58:01Z | Confirmation on screen only, no email | No email service in the stack | Yes |
| 2026-10-29T07:58:01Z | Guests view and cancel, no online change | Vision: view or cancel | Yes |
| 2026-10-29T07:58:01Z | Online cancellation until 14:00 the day before arrival (Europe/Zurich) | Standard check-in 14:00 → 24 h rule | Yes |
| 2026-10-29T07:58:01Z | Guest pages under /booking, public also after login (intent 004) | Guests have no accounts | Yes |
| 2026-10-29T07:58:01Z | Online booking assigns the free room of the type with the lowest number | Guests choose a type, not a room | Yes |

## Scope Changes

| Date | Change | Reason | Impact |
|------|--------|--------|--------|

## Ready for Construction

**Checklist**:
- [x] All requirements documented
- [x] System context defined
- [x] Units decomposed
- [x] Stories created for all units
- [x] Bolts planned
- [x] Human review complete (decisions approved as defaults on 2026-10-29)

## Next Steps

Start after intent 001: `/specsmd-construction-agent --unit="001-guest-booking" --bolt-id="004-guest-booking"`
