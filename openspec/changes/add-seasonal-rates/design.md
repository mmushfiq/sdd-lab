# Design

## Context

See proposal.md for why and specs/ for the required behavior.

- `room_type.nightly_rate` (V2) is the only rate. `reservation.nightly_rate` (V3) copies it when a reservation is
  created (`ReservationService.create`) and when a reservation moves to another room type (`ReservationService.change`).
- Every price is `AvailabilityService.totalPrice(rate, arrival, departure)` = rate × nights. It is called by
  `AvailabilityService.findAvailableRooms`, `ReservationDialog`, `BookingView`, `MyReservationView` and
  `BookingConfirmationEmail`.
- `ReportService.yearReport` walks the nights of each Checked in / Checked out reservation and adds
  `reservation.nightly_rate` per night.
- `FrontDeskService.checkOut` moves the departure date earlier on an early check-out.
- Views format money with `RoomTypeView.formatRate`; `MyReservationView` already reuses `ReservationView.statusLabel`.
- Tests insert reservations through `ReservationTestData.insert` (rate 140.00) and some tests update
  `RESERVATION.NIGHTLY_RATE` directly.

## Goals / Non-Goals

**Goals:**
- One method that calculates the rate of each night of a stay, used by search, booking and changing.
- Each reservation stores the rate of each night, so totals, the price per night and revenue are plain sums.

**Non-Goals:**
- Seasons that repeat every year, copying seasons to the next year, percentage surcharges.
- Rates per weekday, minimum stays, rates per single room.
- Re-pricing existing reservations when a rate changes.
- A history of rate changes.

## Decisions

### D1: Two new tables, `reservation.nightly_rate` dropped

| Table               | Column           | Type           | Constraints                                         |
|---------------------|------------------|----------------|-----------------------------------------------------|
| `seasonal_rate`     | `id`             | BIGINT         | identity, PRIMARY KEY                               |
|                     | `room_type_id`   | BIGINT         | NOT NULL, REFERENCES `room_type (id)`               |
|                     | `name`           | VARCHAR(50)    | NOT NULL                                            |
|                     | `first_night`    | DATE           | NOT NULL                                            |
|                     | `last_night`     | DATE           | NOT NULL, CHECK (`last_night >= first_night`)       |
|                     | `nightly_rate`   | DECIMAL(10, 2) | NOT NULL, CHECK (`nightly_rate > 0`)                |
| `reservation_night` | `reservation_id` | BIGINT         | NOT NULL, REFERENCES `reservation (id)`             |
|                     | `night_date`     | DATE           | NOT NULL                                            |
|                     | `nightly_rate`   | DECIMAL(10, 2) | NOT NULL, CHECK (`nightly_rate > 0`)                |
|                     |                  |                | PRIMARY KEY (`reservation_id`, `night_date`)        |

Index `seasonal_rate_room_type_ix` on `seasonal_rate (room_type_id, first_night)`.

`room_type.nightly_rate` keeps its column name; only the UI label and the specs call it "base rate". Renaming the
column would touch every generated jOOQ reference for no behavior.

Two migrations, so the code compiles and the tests stay green while the readers move to night rows:
- `V7__add_seasonal_rates.sql`: create both tables, then copy every existing reservation into night rows at its
  `nightly_rate`, one row per night from `arrival_date` to the night before `departure_date`, with H2's
  `SYSTEM_RANGE`:
  `INSERT INTO reservation_night SELECT r.id, DATEADD(DAY, n.X, r.arrival_date), r.nightly_rate FROM reservation r
  JOIN SYSTEM_RANGE(0, 3660) n ON n.X < DATEDIFF(DAY, r.arrival_date, r.departure_date)`.
- `V8__drop_reservation_nightly_rate.sql`: `ALTER TABLE reservation DROP COLUMN nightly_rate`, added once no code
  reads or writes it any more.

Alternative considered: keep `reservation.nightly_rate` and store only season periods per reservation. Rejected:
every reader would need period arithmetic; night rows make totals and revenue a `SUM`.

### D2: Rate of each night in `AvailabilityService`

New record `AvailabilityService.NightRate(LocalDate night, BigDecimal rate)` and method
`List<NightRate> nightRates(long roomTypeId, LocalDate arrival, LocalDate departure)`:
- one query for the room type's base rate, one for its seasonal rates with `first_night < departure` and
  `last_night >= arrival`;
- for each night from arrival to the night before departure, the covering seasonal rate, otherwise the base rate.

`static BigDecimal totalPrice(List<NightRate>)` sums the rates; the old `totalPrice(rate, arrival, departure)` is
removed. `AvailableRoom` loses `nightlyRate`; `findAvailableRooms` calculates the night rates once per room type
(the hotel has three) and sets `totalPrice` from them.

### D3: `ReservationService` saves the night rates

- `create`: after inserting the reservation, insert one `reservation_night` row per `nightRates(...)` entry.
- `change`: load the saved rows into a map night → rate. If the room type changes, use `nightRates(...)` of the new
  type for every night. Otherwise take the saved rate for nights that are in the map and the `nightRates(...)` rate
  for new nights. Delete the reservation's rows and insert the new set.
- New `List<NightRate> nights(long reservationId)` ordered by `night_date`, for the views and the email.

### D4: Early check-out removes the nights not stayed

`FrontDeskService.checkOut` deletes the reservation's `reservation_night` rows with `night_date >=` the new
departure date. Totals and revenue then never count nights that were not stayed, without date filters in every
reader.

### D5: Revenue and room-nights from night rows

`ReportService.yearReport` sums `reservation_night` joined with `reservation` (status Checked in or Checked out),
grouped by month of `night_date` within the year: `COUNT(*)` gives room-nights, `SUM(nightly_rate)` gives revenue.
This replaces the loop over dates. Because of D4 the counts stay equal to the nights from arrival to departure.

### D6: Seasonal rates page and service

- `RoomService` gets `findAllSeasonalRates()` (sorted by room type name, first night), `saveSeasonalRate(record)`
  and `deleteSeasonalRate(id)`. `saveSeasonalRate` checks `last_night >= first_night`, locks the room type row
  (`SELECT … FOR UPDATE`, as `ReservationService.checkStay` does for rooms) and rejects an overlap with another
  seasonal rate of the same room type (`first_night <= other.last_night AND last_night >= other.first_night`,
  excluding its own id). Errors are thrown as a new `RoomService.SeasonalRateException` with the spec's message;
  the overlap message names the other seasonal rate.
- New `room/ui/SeasonalRateView` at route `seasonal-rates`, `@RolesAllowed("MANAGER")`, `@AccessDeniedErrorRouter`,
  built like `RoomTypeView`: grid (room type, name, first night, last night, nightly rate), form (room type combo
  box, name, first night and last night date pickers, nightly rate with "CHF" prefix), buttons Save, New seasonal
  rate and Delete (enabled only when a row is selected). Binder validation for required fields, rate > 0 and two
  decimals, last night not before first night.
- `StaffLayout` adds the menu entry "Seasonal rates" after "Room types".

### D7: Display

- `RoomTypeView` and `RoomView`: label "Base rate" for column and field, messages "Base rate is required", "Base rate
  must be greater than zero", "Base rate has at most two decimals".
- `ReservationDialog` (rooms grid) and `BookingView` (room cards): remove the nightly rate; keep the total price.
- New `static String ReservationView.pricePerNight(List<NightRate>)`: groups consecutive nights with the same rate
  in date order, e.g. "2 nights × CHF 140.00, 1 night × CHF 180.00" ("1 night" singular).
- `ReservationDialog` details and `MyReservationView`: "Price per night: …" and "Total price: …" from
  `ReservationService.nights(...)`.
- `BookingConfirmationEmail` and the `BookingView` confirmation: total price from the saved nights.

## Risks / Trade-offs

- [Migration copies existing data] A wrong `SYSTEM_RANGE` join would lose prices → a migration test runs Flyway up
  to V6 on a fresh H2 database, inserts a 3-night reservation, migrates to V7 and checks 3 rows at its old rate.
- [More rows] One row per night (10 rooms × 365 nights ≈ 3,650 rows per year) is small for H2.
- [Concurrent seasonal rate edits] Two managers saving overlapping rates at once → locking the room type row
  serializes the overlap check.
- [Removed nightly rate in search results] Guests and reception see only the total → the price per night is shown
  in the reservation details.
- [Stays longer than 3,660 nights] Not copied completely by the migration; no such stays exist.

## Migration Plan

Flyway applies V7 and V8 on start. No rollback script: the old rate of every reservation lives on in its night rows.

## Open Questions

None.
