---
last_updated: 2026-10-29T11:48:01Z
total_decisions: 3
---

# Decision Index

This index tracks all Architecture Decision Records (ADRs) created during Construction bolts.
Use this to find relevant prior decisions when working on related features.

## How to Use

**For Agents**: Scan the "Read when" fields below to identify decisions relevant to your current task. Before implementing new features, check if existing ADRs constrain or guide your approach. Load the full ADR for matching entries.

**For Humans**: Browse decisions chronologically or search for keywords. Each entry links to the full ADR with complete context, alternatives considered, and consequences.

---

## Decisions

### ADR-003: Anonymize guest data in place
- **Status**: accepted
- **Date**: 2026-10-29
- **Bolt**: 003-reservation (reservation)
- **Path**: `bolts/003-reservation/adr-003-anonymize-guest-data-in-place.md`
- **Summary**: Guests may ask the hotel to erase their personal data while reports still need the stays. Anonymization overwrites name, email and phone with "Anonymized" and sets `anonymized = true`, keeping the row and all non-personal data.
- **Read when**: Storing, copying, exporting or emailing guest personal data, building reports on reservations, or adding retention/erasure rules

### ADR-002: Reservation queries read room catalog tables directly
- **Status**: accepted
- **Date**: 2026-10-29
- **Bolt**: 002-reservation (reservation)
- **Path**: `bolts/002-reservation/adr-002-direct-reads-of-room-catalog-tables.md`
- **Summary**: Availability and booking need room, room type and reservation data in one query and a lock on the room row. Feature services may read and lock another feature's tables directly with jOOQ; writes still go through the owning service.
- **Read when**: Writing a query or service that needs data owned by another feature (rooms, reservations, staff), e.g. front desk, housekeeping, reports, online booking

### ADR-001: Room-row lock against double booking
- **Status**: accepted
- **Date**: 2026-10-29
- **Bolt**: 002-reservation (reservation)
- **Path**: `bolts/002-reservation/adr-001-room-row-lock-against-double-booking.md`
- **Summary**: A room must never have two CONFIRMED reservations with overlapping nights, even under concurrent saves. Every create or move of a reservation locks the room row (FOR UPDATE), re-checks overlap and capacity, then writes, all in one transaction; extended in bolt 003 with reservation-row locks for lifecycle changes, the lock order reservation → room, and locking the room before setting it out of service.
- **Read when**: Creating, changing, cancelling or moving reservations, setting rooms out of service, online booking, check-in that changes rooms, or any concurrency question about bookings
