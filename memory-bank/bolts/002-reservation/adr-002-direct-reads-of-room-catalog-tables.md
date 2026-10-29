---
bolt: 002-reservation
created: 2026-10-29T10:51:10Z
status: accepted
superseded_by: null
---

# ADR-002: Reservation queries read room catalog tables directly

## Context

Availability needs rooms, their type's capacity and rate, the out-of-service flag and overlapping reservations at once, and booking must lock the room row. Both units share one H2 schema, and `RoomCatalogService` returns Java lists, not query building blocks.

## Decision

Feature services may read (and lock) another feature's tables directly with jOOQ when a single query or a row lock needs them. Writes to another feature's tables still go through that feature's service. The reservation unit therefore joins `room` and `room_type` in its own queries and leaves `RoomCatalogService` unchanged.

## Rationale

One SQL query with `NOT EXISTS` keeps search correct and fast, and the `FOR UPDATE` lock (ADR-001) has to run in the booking transaction on the `room` row. Calling `RoomCatalogService.findBookableRooms` and filtering in Java would load every room and still not give the lock.

### Alternatives Considered

| Alternative | Pros | Cons | Why Rejected |
|-------------|------|------|--------------|
| Call `RoomCatalogService` and filter in Java | Clear unit boundary | Two round trips, logic split across units, no row lock | Can't provide the ADR-001 lock |
| Add lock/availability methods to `RoomCatalogService` | Boundary kept | Room catalog learns about reservations; circular knowledge | Wrong direction of dependency |
| Database view owned by room catalog | Stable read contract | Extra schema object to maintain for one query | Overhead without current benefit |

## Consequences

### Positive

- Availability is one indexed query; booking can lock the room row
- No change to the room catalog unit

### Negative

- Schema changes to `room` or `room_type` can break reservation queries; the jOOQ-generated classes make this a compile error rather than a runtime one

### Risks

- Units drifting into writing each other's tables. Mitigation: the "reads only, writes through the owning service" rule above.

## Related

- **Stories**: 002-reservation/001-search-availability, 002-create-reservation
- **Standards**: candidate for `standards/coding-standards.md` (File Organization conventions)
- **Previous ADRs**: ADR-001
