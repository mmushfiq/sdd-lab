---
stage: design
bolt: 003-reservation
created: 2026-10-29T11:13:14Z
---

## Technical Design: reservation (lifecycle)

### Architecture Pattern

Unchanged from bolt `002-reservation`: layered feature packages, views call one service per feature, services run jOOQ queries (`standards/coding-standards.md`). Applies ADR-001 (room-row lock for every write that puts a reservation on a room or takes a room out of service) and ADR-002 (room catalog reads the reservation table directly; writes stay with the owning service).

### Layer Structure

```text
┌──────────────────────────────────────────────────────────────────┐
│ Presentation   ReservationListView (+ Change / Cancel / Anonymize│  Vaadin Flow
│                actions), ChangeReservationDialog, GuestFields,   │
│                BookingDialog (now uses GuestFields), RoomView    │
├──────────────────────────────────────────────────────────────────┤
│ Application    ReservationService: change, cancel, anonymize     │  Spring
│                RoomCatalogService: deleteRoom / saveRoom guards  │
├──────────────────────────────────────────────────────────────────┤
│ Domain         ReservationListItem (+ rule methods changeable(), │  records
│                cancellable(), anonymizable()), StayPeriod, Guest │
│                exceptions (see below)                            │
├──────────────────────────────────────────────────────────────────┤
│ Infrastructure jOOQ on reservation, room, room_type; no new      │  jOOQ + H2
│                migration (anonymized column exists since V2)     │
└──────────────────────────────────────────────────────────────────┘
```

### API Design

No REST API. Java service contract:

**ReservationService** (all `@Transactional`):

- **`changeReservation(String bookingNumber, long roomId, StayPeriod period, int numberOfGuests, Guest guest)`**
  1. Lock the reservation row (`SELECT ... FOR UPDATE` by booking number); missing or not changeable → `ReservationNotModifiableException("Reservation can no longer be changed")`
  2. Date rules: new arrival ≥ today unless equal to the current arrival; departure ≥ today → `IllegalArgumentException` (UI validates first)
  3. Lock the target room row (ADR-001); out of service → `RoomNotAvailableException("Room is not available for these dates")`; guests > capacity → `CapacityExceededException`
  4. Overlap check on the target room excluding the reservation's own id → `RoomNotAvailableException("Room is not available for these dates")`
  5. Update; total price = current rate × nights only if room or dates changed
- **`cancelReservation(String bookingNumber)`**: lock; not CONFIRMED → `ReservationNotModifiableException("Reservation can no longer be cancelled")`; set CANCELLED
- **`anonymizeGuest(String bookingNumber)`**: lock; not anonymizable → `ReservationNotModifiableException("Reservation cannot be anonymized")`; set name, email, phone to "Anonymized" and `anonymized = true`
- `findReservations(filter)`: unchanged criteria; the returned item gains the fields needed for actions

Lock order is always reservation row → room row, and booking or room-out-of-service only lock the room row, so no deadlock cycle is possible.

**RoomCatalogService** (extended):

- **`deleteRoom(long id)`**: any reservation for the room → `RoomHasReservationsException` "Room has reservations and cannot be deleted; set it out of service instead"
- **`saveRoom(RoomRecord room)`** with `outOfService = true` on an existing room: lock the room row first (ADR-001), then collect booking numbers of CONFIRMED reservations with departure > today → `RoomHasFutureReservationsException` "Room has future reservations and cannot be set out of service: {numbers}"

**Value objects / records**:

- `ReservationListItem` gains `roomId`, `guestEmail`, `guestPhone`, `anonymized`; adds `changeable()`, `cancellable()`, `anonymizable()` (rules from the domain model, today = `LocalDate.now()`). The service evaluates the same methods on the freshly locked row, so the UI and the service share one implementation of the rules.
- `RoomNotAvailableException` gets a message constructor (booking keeps "Room is no longer available for these dates")

### Data Model

- **reservation**: no schema change. `anonymized` (since V2) is set to true; the email column is VARCHAR without format check, so "Anonymized" fits.
- Queries added:
  - Lock reservation: `SELECT id FROM reservation WHERE booking_number = ? FOR UPDATE`, then the list query by booking number
  - Overlap excluding self: the existing condition plus `id <> :ownId`
  - Room protection: `count(*)` of reservations by room; booking numbers with `room_id = ? AND status = 'CONFIRMED' AND departure > today`
- All existence checks use `count(*)` (H2 stale-EXISTS issue, test report of bolt 002)

### UI Design

- **ReservationListView**: single-select grid; buttons "Change", "Cancel reservation", "Anonymize" above the grid, enabled only when the selected item allows the action. Status column shows "CANCELLED" / "CONFIRMED", with " (anonymized)" appended when anonymized.
- **ChangeReservationDialog**: arrival, departure, room (combo box of all rooms: number and type), guests, then guest fields; prefilled. Save → "Reservation changed" notification, close, refresh. `RoomNotAvailableException` → notification, dialog stays open; `CapacityExceededException` → field error on guests. Arrival field read-only when the arrival is already today or earlier (staying guest).
- **Cancel / Anonymize**: `ConfirmDialog` with "Yes" / "No". Anonymize text says it cannot be undone. Success → notification, refresh.
- **GuestFields**: name, email, phone with the validation from bolt 002, extracted so BookingDialog and ChangeReservationDialog share it.
- **RoomView**: catches the two new room exceptions on delete / save and shows them as notifications.

### Security Design

- **Personal data**: anonymization overwrites the data in place; log lines contain booking numbers only. The removed values are never logged or kept elsewhere.
- **Integrity**: state rules checked again under the row lock, not trusted from the UI (enabled buttons are convenience only).
- **Access**: still open until intent 004.

### NFR Implementation

- **No double booking on change**: change locks the target room like a new booking. Test: a change and a new booking for the same room and nights in parallel → exactly one succeeds.
- **No booked room taken out of service**: out-of-service locks the room before checking. Test: a booking and an out-of-service change in parallel → never both succeed.
- **Response time**: a handful of indexed statements per action.

### Integration

- **001-room-catalog**: `RoomCatalogService` reads `reservation` (ADR-002). The reservation unit reads the room list through `RoomCatalogService.findRooms()` for the change dialog's room combo box.
- **Intent 002 (online booking)**: will reuse `cancelReservation` with its own 24-hour rule on top, and the find-by-booking-number path.
