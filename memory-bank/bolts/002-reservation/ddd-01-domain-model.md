---
stage: model
bolt: 002-reservation
created: 2026-10-29T10:43:22Z
---

## Static Model: reservation

### Entities

- **Reservation** (aggregate root): id, bookingNumber, roomId, stayPeriod, numberOfGuests, guest, totalPrice (CHF), status, anonymized - Rules:
  - Created with status CONFIRMED; `anonymized` starts false (used by bolt 003)
  - numberOfGuests ≥ 1 and ≤ capacity of the booked room's type
  - totalPrice = nightly rate × nights, fixed at booking; later rate changes never touch it
  - bookingNumber never changes after creation
  - Only CONFIRMED reservations block a room; CANCELLED ones are history

- **Room** (owned by unit `001-room-catalog`, referenced by id only): number, room type (capacity, nightly rate), outOfService - Rule: only in-service rooms can be offered or booked

### Value Objects

- **StayPeriod**: arrival, departure (dates) - Constraints:
  - departure > arrival (at least 1 night)
  - nights() = days between arrival and departure
  - overlaps(other) on the half-open interval [arrival, departure): `this.arrival < other.departure && this.departure > other.arrival`, so a departure day may be the next guest's arrival day
  - "arrival not in the past" applies to new searches and bookings, not to the value itself (stored reservations age into the past)

- **Guest**: name, email, phone - Constraints: all three required; email must be a valid address; stored as entered (case kept), compared case-insensitively later

- **BookingNumber**: 8 characters from `ABCDEFGHJKLMNPQRSTUVWXYZ23456789` (no 0/O, no 1/I) - Constraints: random (not sequential, not guessable), unique; lookups ignore case

- **ReservationStatus**: CONFIRMED, CANCELLED

- **AvailableRoom** (search result): roomId, number, roomTypeName, capacity, nightlyRate, totalPrice for the searched period

- **ReservationFilter**: bookingNumber?, guestName?, from?, to?, includeCancelled - Rules:
  - No criteria: CONFIRMED reservations with departure ≥ today
  - bookingNumber given: exactly that reservation, any status
  - guestName: contains, case-insensitive
  - from/to: stays overlapping the range (same half-open rule as StayPeriod)
  - includeCancelled: CANCELLED reservations are added

### Aggregates

- **Reservation**: Members: StayPeriod, Guest, BookingNumber, ReservationStatus - Invariants:
  - Valid StayPeriod, valid Guest, 1 ≤ numberOfGuests ≤ room capacity
  - Total price fixed at creation
- **Cross-aggregate invariant (no double booking)**: for one room, no two CONFIRMED reservations have overlapping stay periods. A single Reservation cannot guarantee this, so the booking service enforces it by serializing bookings per room (lock the room, re-check, then insert).

### Domain Events

- **ReservationCreated**: Trigger: a reservation is stored - Payload: bookingNumber, roomId, stayPeriod
  - No consumer yet; recorded as an info log line (without guest data). Later intents (emails, reports) can turn it into a real event.

### Domain Services

- **ReservationService**: Operations:
  - `searchAvailability(stayPeriod, numberOfGuests)` → list of AvailableRoom: in-service rooms with capacity ≥ guests and no overlapping CONFIRMED reservation, sorted by room number
  - `createReservation(roomId, stayPeriod, numberOfGuests, guest)` → Reservation; fails with *RoomNotAvailable* when the room is out of service or an overlapping CONFIRMED reservation exists, *CapacityExceeded* when guests exceed capacity
  - `findReservations(filter)` → list of reservations sorted by arrival
  - Dependencies: Room catalog (rooms, capacity, rate, out-of-service), ReservationRepository, BookingNumber generator
- **BookingNumber generator**: Operations: `next()` → random BookingNumber; the caller retries on the rare uniqueness collision

### Repository Interfaces

- **ReservationRepository** (contract; how it is realized is decided in Stage 2): Entity: Reservation - Methods:
  - `lockRoom(roomId)` - serialize bookings of one room for the current transaction
  - `existsOverlapping(roomId, stayPeriod)` - any CONFIRMED overlap
  - `insert(reservation)`
  - `find(filter)`
  - `findAvailableRooms(stayPeriod, numberOfGuests)`

### Ubiquitous Language

- **Reservation / booking**: one room reserved for one stay by one guest
- **Booking number**: the 8-character code a guest and staff use to identify a reservation
- **Stay period**: the nights from arrival to departure; the departure night is not included
- **Night**: one date in the stay period; price is per night
- **Overlap**: two stay periods share at least one night
- **Back-to-back**: one stay departs on the day the next arrives; allowed
- **Available room**: in service, big enough, and no overlapping confirmed reservation
- **Confirmed / cancelled**: confirmed reservations hold the room; cancelled ones don't
- **Double booking**: two confirmed reservations for the same room with overlapping nights; must never happen

### Story Coverage

- **001-search-availability**: StayPeriod (validation, nights, overlap), AvailableRoom, `searchAvailability`
- **002-create-reservation**: Reservation aggregate, Guest, BookingNumber, `createReservation`, room lock + re-check, fixed total price, ReservationCreated
- **003-find-reservations**: ReservationFilter, `findReservations`
