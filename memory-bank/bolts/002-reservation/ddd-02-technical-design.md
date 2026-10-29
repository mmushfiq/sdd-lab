---
stage: design
bolt: 002-reservation
created: 2026-10-29T10:46:42Z
---

## Technical Design: reservation

### Architecture Pattern

Layered feature package, the same shape as `roomcatalog` (see `standards/coding-standards.md`): Vaadin views call one Spring `@Service`; the service holds the domain rules and runs jOOQ queries directly. No separate repository classes: the repository contract from the domain model is realized as private jOOQ query methods in the service, as the standards prescribe ("services run jOOQ queries"). Value objects are Java records.

### Layer Structure

```text
┌──────────────────────────────────────────────────────────────┐
│ Presentation   AvailabilityView, ReservationListView,        │  Vaadin Flow
│                BookingDialog (Binder validation)             │
├──────────────────────────────────────────────────────────────┤
│ Application    ReservationService (@Transactional use cases: │  Spring
│                search, create, find)                         │
├──────────────────────────────────────────────────────────────┤
│ Domain         StayPeriod, Guest, ReservationStatus,         │  records/enum
│                AvailableRoom, ReservationFilter,             │
│                ReservationListItem, BookingNumbers,          │
│                RoomNotAvailableException,                    │
│                CapacityExceededException                     │
├──────────────────────────────────────────────────────────────┤
│ Infrastructure jOOQ queries on reservation, room, room_type  │  jOOQ + H2
│                (generated classes), Flyway V2                │
└──────────────────────────────────────────────────────────────┘
```

Package: `ch.martinelli.edu.hrs.reservation`. `MainLayout` gets two new side-nav entries.

### API Design

No REST API (Vaadin Flow calls services in-process, `standards/tech-stack.md`). The contract is the Java service API:

- **`searchAvailability(StayPeriod period, int numberOfGuests)`** → `List<AvailableRoom>` sorted by room number
  - Rejects `numberOfGuests < 1` and arrival before today with `IllegalArgumentException` (the UI validates first)
- **`createReservation(long roomId, StayPeriod period, int numberOfGuests, Guest guest)`** → `String` booking number
  - `RoomNotAvailableException` ("Room is no longer available for these dates") when the room is out of service or an overlapping CONFIRMED reservation exists
  - `CapacityExceededException` when guests exceed the room's capacity
- **`findReservations(ReservationFilter filter)`** → `List<ReservationListItem>` sorted by arrival, then booking number

Value objects:

- `StayPeriod(LocalDate arrival, LocalDate departure)` - compact constructor rejects departure ≤ arrival; `nights()`, `overlaps(StayPeriod)`
- `Guest(String name, String email, String phone)`
- `ReservationFilter(String bookingNumber, String guestName, LocalDate from, LocalDate to, boolean includeCancelled)` - nulls mean "not set"
- `AvailableRoom(long roomId, String number, String roomTypeName, int capacity, BigDecimal nightlyRate, BigDecimal totalPrice)`
- `ReservationListItem(String bookingNumber, String guestName, String roomNumber, LocalDate arrival, LocalDate departure, int numberOfGuests, BigDecimal totalPrice, ReservationStatus status)`

### Data Model

- **reservation** (Flyway `V2__create_reservation.sql`): Columns:
  - `id` BIGINT identity PK
  - `booking_number` CHAR(8) NOT NULL, unique
  - `room_id` BIGINT NOT NULL
  - `arrival` DATE NOT NULL, `departure` DATE NOT NULL, CHECK (departure > arrival)
  - `number_of_guests` INT NOT NULL, CHECK (number_of_guests ≥ 1)
  - `guest_name` VARCHAR(100), `guest_email` VARCHAR(254), `guest_phone` VARCHAR(30), all NOT NULL
  - `total_price` DECIMAL(10,2) NOT NULL
  - `status` VARCHAR(10) NOT NULL, CHECK IN ('CONFIRMED', 'CANCELLED')
  - `anonymized` BOOLEAN NOT NULL DEFAULT FALSE
  - `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
  - Relationships: FK `room_id` → `room(id)`
  - Index on (room_id, arrival) for the overlap check
- Status stored as text and mapped to the `ReservationStatus` enum in the service.

Key queries:

- **Availability**: rooms joined with room_type where `out_of_service = false`, `capacity ≥ guests`, and `NOT EXISTS` a reservation with `room_id = room.id AND status = 'CONFIRMED' AND arrival < :departure AND departure > :arrival`. Total price = `nightly_rate × nights` computed in Java.
- **Create** (one transaction):
  1. `SELECT ... FROM room WHERE id = :roomId FOR UPDATE` - locks the room row; a second booking for the same room waits here until the first commits
  2. Read capacity and rate; reject out of service or too many guests
  3. Re-check the overlap with the same `EXISTS` condition
  4. Generate a booking number (re-draw while one exists), insert with status CONFIRMED and the computed total price
- **Find**: default `status = 'CONFIRMED' AND departure ≥ today`; booking number filter → `upper(booking_number) = upper(:n)` only, any status; guest name → `lower(guest_name) LIKE %lower(:name)%`; range → `arrival < :to AND departure > :from`; includeCancelled widens the status condition.

### UI Design

- **AvailabilityView** (route `availability`): arrival and departure date pickers, guests field, Search button; results grid (number, type, capacity, nightly rate, total price); "Book" on a selected row opens the booking dialog. Empty result → "No rooms available for these dates".
- **BookingDialog**: shows room and period; fields guests (prefilled), name, email, phone; Save → notification "Reservation confirmed: {booking number}", closes, refreshes the search. `RoomNotAvailableException` → notification, search refreshed. `CapacityExceededException` → field error on guests.
- **ReservationListView** (route `reservations`): booking number, guest name, from, to, "Show cancelled" checkbox, filter on change; grid (booking number, guest, room, arrival, departure, guests, total, status); empty → "No reservations found".

### Security Design

- **Access**: staff screens stay open until intent 004 (login and roles), as in `tech-stack.md`.
- **Booking number**: drawn from `SecureRandom`; ~32⁸ ≈ 10¹² combinations, so it can't be guessed (needed for guest self-service in intent 002).
- **Personal data**: never logged; the ReservationCreated log line carries booking number, room and dates only.
- **Input**: Binder validation in the UI and argument checks in the service (trust boundary); all SQL through jOOQ bind values.

### NFR Implementation

- **Zero double bookings**: room-row lock (`FOR UPDATE`) + re-check inside one `@Transactional` method. Proven by a service test that runs two bookings for the same room and nights in parallel threads and expects exactly one success and one `RoomNotAvailableException`.
- **Search and save < 1 s**: ~3,000 reservations/year and 10 rooms; one indexed query per search, a handful of statements per booking.
- **Phone booking < 1 minute**: search → book → booking number in one view and one dialog; covered by a Playwright IT of that flow.
- **Testability**: dates in tests are relative to today, so no clock abstraction is needed yet.

### Integration

- **001-room-catalog**: reads `room` and `room_type` directly via jOOQ in the same schema (needed for the single availability query and for the row lock). `RoomCatalogService` stays unchanged.
- **003-reservation** (next bolt): change, cancel, room protection and anonymization will extend this service and the list view.
