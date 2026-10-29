---
stage: model
bolt: 003-reservation
created: 2026-10-29T11:08:26Z
---

## Static Model: reservation (lifecycle)

Builds on the model of bolt `002-reservation` (Reservation, StayPeriod, Guest, BookingNumber, ReservationStatus). Only additions and new rules are listed here.

### Entities

- **Reservation** (aggregate root, extended): new behaviour change, cancel, anonymize - Rules:
  - Today is the reference date for "past" and "future"
  - **Changeable** ⇔ status CONFIRMED and not anonymized and departure ≥ today
  - **Cancellable** ⇔ status CONFIRMED (any time, also during the stay; reception only)
  - **Anonymizable** ⇔ not anonymized and (status CANCELLED or departure < today)
  - A changeable reservation is never anonymizable and vice versa (for CONFIRMED ones)
  - bookingNumber never changes; a cancelled or anonymized reservation is never deleted

- **Room** (owned by `001-room-catalog`), new rules from FR-2:
  - **Deletable** ⇔ no reservation at all for the room (any status, past or future)
  - **Can go out of service** ⇔ no CONFIRMED reservation with departure > today ("departs today" does not block)

### Value Objects

- **StayPeriod**: unchanged; its overlap rule is reused for changes
- **ReservationChange**: new stayPeriod, roomId, numberOfGuests, guest - Constraints:
  - All FR-3/FR-4 rules apply: departure > arrival, room in service, guests ≤ capacity, no overlap with other CONFIRMED reservations of the room
  - New arrival must be today or later, **unless it equals the current arrival** (a staying guest keeps the arrival in the past and may only move the departure)
  - New departure ≥ today
- **ANONYMIZED placeholder**: the text "Anonymized" replacing name, email and phone

### Aggregates

- **Reservation**: Invariants (in addition to bolt 002):
  - Changes only while changeable; cancel only while CONFIRMED; anonymize only while anonymizable
  - Total price is recalculated with the current nightly rate **only** when the dates or the room change; changing guests or guest data keeps the price
  - A reservation never conflicts with itself: the overlap check excludes its own id
  - After anonymization, name, email and phone are "Anonymized", `anonymized` is true; booking number, room, dates, guests, price and status stay
- **Cross-aggregate invariants** (enforced by services under the room lock of ADR-001):
  - No double booking, also when a reservation is moved to other dates or another room
  - A room is never out of service while it has future CONFIRMED reservations, even when a booking and the out-of-service change happen at the same moment
  - A room with reservations is never deleted

### Domain Events

All logged at info, without guest data (no consumer yet):

- **ReservationChanged**: Trigger: a change is saved - Payload: bookingNumber, roomId, stayPeriod, priceRecalculated
- **ReservationCancelled**: Trigger: status set to CANCELLED - Payload: bookingNumber
- **GuestDataAnonymized**: Trigger: anonymization - Payload: bookingNumber only (never the removed data)

### Domain Services

- **ReservationService** (extended): Operations:
  - `findReservation(bookingNumber)` → one reservation with its full data, for the change dialog
  - `changeReservation(bookingNumber, change)` → fails with *ReservationNotChangeable*, *RoomNotAvailable*, *CapacityExceeded* or a date error
  - `cancelReservation(bookingNumber)` → fails with *ReservationNotCancellable*
  - `anonymizeGuest(bookingNumber)` → fails with *ReservationNotAnonymizable*
- **RoomCatalogService** (extended, in `001-room-catalog`): Operations:
  - `deleteRoom` fails with *RoomHasReservations*: "Room has reservations and cannot be deleted; set it out of service instead"
  - `saveRoom` setting out of service fails with *RoomHasFutureReservations* carrying the affected booking numbers
  - Dependency: reads the reservation table (ADR-002); does not call ReservationService, so there is no cycle

### Repository Interfaces

- **ReservationRepository** (contract, extended): Entity: Reservation - Methods:
  - `lockReservation(bookingNumber)` - read the reservation for update, so a change and a cancel can't overwrite each other
  - `lockRoom(roomId)` - as in bolt 002; also used before setting a room out of service
  - `existsOverlapping(roomId, stayPeriod, excludingReservationId)`
  - `update(reservation)`
  - `futureConfirmedBookingNumbers(roomId)`, `existsAnyForRoom(roomId)` - used by the room catalog

### Ubiquitous Language

- **Change**: new dates, room, number of guests or guest data for an existing reservation; booking number stays
- **Cancel**: set status CANCELLED; the reservation is kept and the nights are free again
- **Anonymize**: irreversibly replace a guest's personal data with "Anonymized" (GDPR right to erasure)
- **Staying guest**: arrival today or earlier and departure today or later
- **Future reservation** (for room rules): CONFIRMED with departure after today
- **Past reservation**: departure before today

### Story Coverage

- **004-change-reservation**: Changeable rule, ReservationChange, self-exclusion, price-recalculation rule, room lock, staying-guest rule
- **005-cancel-reservation**: Cancellable rule, ReservationCancelled
- **006-protect-rooms-with-reservations**: Room deletable / out-of-service rules, lock before out-of-service, booking-number message
- **007-anonymize-guest-data**: Anonymizable rule, placeholder, GuestDataAnonymized without data

### Decision (approved 2026-10-29)

- FR-6 says changes need "a departure date in the future", while story 004 only excludes "departure in the past". This model follows the story: **departure today is still changeable**, so a guest on their departure day can extend the stay. This also makes "changeable" and "anonymizable" exact opposites for CONFIRMED reservations, as story 007 expects.
