---
intent: 001-rooms-and-reservations
created: 2026-10-29T07:18:10Z
completed: 2026-10-29T07:40:42Z
status: complete
---

# Inception Log: 001-rooms-and-reservations

## Overview

**Intent**: Rooms, room types and rates; availability search; reception creates, changes and cancels reservations without double bookings.
**Type**: green-field
**Created**: 2026-10-29T07:18:10Z
**Source**: `docs/hotel-requirements.md`

## Artifacts Created

| Artifact | Status | File |
|----------|--------|------|
| Requirements | ✅ | requirements.md |
| System Context | ✅ | system-context.md |
| Units | ✅ | units.md, units/*/unit-brief.md |
| Stories | ✅ | units/*/stories/*.md (11) |
| Bolt Plan | ✅ | memory-bank/bolts/001-room-catalog, 002-reservation, 003-reservation |

## Summary

| Metric | Count |
|--------|-------|
| Functional Requirements | 8 |
| Non-Functional Requirements | 6 categories |
| Units | 2 |
| Stories | 11 |
| Bolts Planned | 3 |

## Units Breakdown

| Unit | Stories | Bolts | Priority |
|------|---------|-------|----------|
| 001-room-catalog | 4 | 1 | Must |
| 002-reservation | 7 | 2 | Must |

## Decision Log

| Date | Decision | Rationale | Approved |
|------|----------|-----------|----------|
| 2026-10-29T07:18:10Z | Split the vision into 5 intents: 001 rooms-and-reservations, 002 online-booking, 003 check-in-check-out-housekeeping, 004 staff-login-and-roles, 005 reports | Vision is too large for one intent; reservations are the core everything else builds on | Yes |
| 2026-10-29T07:18:10Z | Staff login (Spring Security + Vaadin login, roles) built in intent 004; screens open until then; `tech-stack.md` updated | Keeps the first intents focused on the domain | Yes |
| 2026-10-29T07:18:10Z | Guests find their reservation with booking number + email, no accounts | Simplest flow for guests; fewer personal data stored | Yes |
| 2026-10-29T07:18:10Z | One fixed nightly rate per room type; price = rate × nights, stored on the reservation | No seasonal pricing in vision | Yes |
| 2026-10-29T07:18:10Z | Guests cancel online up to 24 h before arrival, free; reception any time | Default cancellation policy (online part in intent 002) | Yes |
| 2026-10-29T07:18:10Z | One reservation = one room; name, email, phone required | Simple model for a ten-room hotel | Yes |
| 2026-10-29T07:18:10Z | Rooms have an "out of service" flag | Block rooms for repairs | Yes |
| 2026-10-29T07:18:10Z | GDPR: store only name, email, phone; anonymize on request | Data minimization, right to erasure | Yes |
| 2026-10-29T07:20:41Z | Vertical units (domain + Vaadin view per unit), no separate `-ui` frontend unit | Vaadin Flow UI is server-side Java in the same feature package; a separate UI unit would split every story | Yes |
| 2026-10-29T07:20:41Z | FR-2 reservation-dependent rules delivered in 002-reservation story 006 | Reservations don't exist before that unit | Yes |
| 2026-10-29T07:20:41Z | Double-booking protection by locking the room row in the booking transaction | H2 has no exclusion constraints | Yes |

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
- [x] Human review complete

## Next Steps

1. Begin Construction Phase
2. Start with Unit: 001-room-catalog, bolt 001-room-catalog
3. Execute: `/specsmd-construction-agent --unit="001-room-catalog" --bolt-id="001-room-catalog"`

## Dependencies

001-room-catalog (bolt 001) → 002-reservation (bolt 002 → bolt 003)
