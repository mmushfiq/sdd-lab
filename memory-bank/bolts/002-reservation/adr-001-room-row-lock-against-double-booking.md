---
bolt: 002-reservation
created: 2026-10-29T10:51:10Z
status: accepted
superseded_by: null
---

# ADR-001: Room-row lock against double booking

## Context

A room must never have two CONFIRMED reservations with overlapping nights, even when two receptionists (or later, a guest online) save at the same moment. H2 has no exclusion constraints, and a plain "check, then insert" lets two transactions both pass the check before either commits.

## Decision

Every operation that creates or moves a reservation onto a room runs in one `@Transactional` service method that:

1. Locks the room row with `SELECT ... FROM room WHERE id = ? FOR UPDATE`
2. Re-checks out-of-service, capacity and overlapping CONFIRMED reservations (half-open interval)
3. Inserts or updates the reservation

A conflict raises `RoomNotAvailableException`. A concurrency test with two parallel bookings for the same room and nights must show exactly one success.

## Rationale

The room row is the natural serialization point: bookings for different rooms never wait for each other, and the lock is released at commit. Under H2's default READ COMMITTED, the waiting transaction sees the first one's reservation once it gets the lock, so its re-check fails correctly.

### Alternatives Considered

| Alternative | Pros | Cons | Why Rejected |
|-------------|------|------|--------------|
| Database exclusion constraint | Enforced by the DB for every writer | Not available in H2 | Not possible with the chosen database |
| One row per booked night with a unique (room, night) constraint | DB-enforced, no locking | Extra table, many rows per stay, more complex change/cancel | More code and data for the same guarantee |
| SERIALIZABLE isolation | No explicit lock | Behaviour and retry handling are engine-specific; failures surface as generic errors | Harder to reason about and test |
| Application-level lock (`synchronized`, per-room mutex) | Simple | Breaks with more than one app instance; bypassed by other writers | Not safe beyond one JVM |

## Consequences

### Positive

- Zero double bookings, provable with a concurrency test
- Only bookings for the same room wait; others run in parallel
- Plain SQL, easy to read in the service

### Negative

- Every write path that books a room must follow the pattern; skipping the lock reintroduces the race
- Concurrent bookings for one room are serialized (no practical impact at hotel scale)

### Risks

- A long transaction holding the lock makes the other wait up to H2's lock timeout and then fail. Mitigation: keep the locked section to a few short statements, no user interaction inside the transaction.

## Related

- **Stories**: 002-reservation/002-create-reservation; later 004-change-reservation, 002-online-booking/002-book-room-online
- **Standards**: candidate for `standards/data-stack.md` once a second write path uses it
- **Previous ADRs**: none
