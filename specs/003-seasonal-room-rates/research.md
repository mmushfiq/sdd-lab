# Research: Seasonal Room Rates

All Technical Context items were known from the code; no NEEDS CLARIFICATION remained. The decisions below
settle how the spec is met in the existing code.

## R1: Store the rate of each night in its own table

- **Decision**: New table `reservation_night (reservation_id, night_date, nightly_rate)`, one row per night,
  primary key `(reservation_id, night_date)`. `reservation.total_price` stays and is always the sum of the
  reservation's night rates. `reservation.nightly_rate` is dropped once no code reads it.
- **Rationale**: FR-006 needs the rate of every night, frozen at booking. With one row per night the total, the
  price per night (FR-005), the change rule (FR-007) and monthly revenue (FR-009) are plain sums or lookups by
  date. A stay has at most 30 nights (001 FR-008) and the hotel ten rooms, so the table stays small (~3,650 rows
  per year).
- **Alternatives considered**:
  - Keep one nightly rate plus the seasonal periods a reservation crossed: every reader would need period
    arithmetic, and a later change of a seasonal rate would change old prices unless the periods were copied.
  - Store only the total price: the price per night and revenue per month could not be derived.
  - A JSON column of rates: not queryable by month for revenue; H2/jOOQ support adds friction.

## R2: Two migrations, so every user story keeps the build green

- **Decision**:
  - `V5__seasonal_rates.sql`: create `seasonal_rate` and `reservation_night`; copy every existing reservation
    into night rows at its `nightly_rate` (FR-008): one row per date from `arrival_date` up to the day before
    `departure_date`, joined with H2's `SYSTEM_RANGE(0, 29)` (no stay is longer than 30 nights, 001 FR-008).
  - `V6__drop_reservation_nightly_rate.sql`: `alter table reservation drop column nightly_rate`, added in the
    last story, after every reader and writer moved to night rows.
- **Rationale**: Dropping the column in V5 would break the compile of every class that uses
  `RESERVATION.NIGHTLY_RATE` at once (generated jOOQ code changes on `./mvnw compile`), so the user stories
  could not be delivered one by one. Migrations V1–V4 stay untouched (constitution III).
- **Alternatives considered**: Keep `nightly_rate` forever as "rate of the first night": a misleading column
  that invites wrong revenue code. One migration with the drop: see rationale.

## R3: Rate of each night in `AvailabilityService`

- **Decision**: New record `reservation/NightRate(LocalDate night, BigDecimal rate)`. New method
  `AvailabilityService.nightRates(long roomTypeId, LocalDate arrival, LocalDate departure)`: reads the room
  type's standard rate and its seasonal rates with `start_date < departure and end_date >= arrival`, and returns
  one `NightRate` per night: the covering seasonal rate, otherwise the standard rate (FR-004).
  `search()` reads the seasonal rates of all room types for the stay once and prices each room from them.
  `AvailableRoom` loses `nightlyRate`; `totalPrice` is the sum.
- **Rationale**: One rule for search, booking and change, so the price shown in the search is the price saved.
  A night is identified by the date it starts on, so the end date is inclusive (FR-002) and the departure day is
  never priced, as in `AvailabilityService.blocks`.
- **Alternatives considered**: An SQL query per night: 30 queries per room for a long stay. A pricing service of
  its own: one more Spring bean for one method that needs the same stay rules as `AvailabilityService`.

## R4: Change keeps the rates of nights the reservation already had

- **Decision**: `ReservationService.apply` no longer takes `reprice`. It builds the new night set:
  - same room type as before (or a new booking): for each night, the saved rate if the reservation already had
    that night, otherwise the rate from `nightRates` (FR-007);
  - another room type: every night from `nightRates` of the new room type.
  It deletes the reservation's night rows, inserts the new set and sets `total_price` to their sum. A move to
  another room of the same type with the same dates therefore keeps every rate (US3/AC5); a shortened stay
  drops the removed nights (edge case).
- **Rationale**: Matches the clarification (session 2026-11-08) and the OpenSpec branch. Replacing all rows is
  simpler than diffing them, and runs under the room lock that `apply` already takes.
- **Alternatives considered**: Update only added/removed rows: more code, same result.

## R5: Seasonal rate maintenance

- **Decision**: New `room/SeasonalRateService` (pattern of `RoomTypeService`): `findAll()` sorted by room type
  name and start date, `save(record)`, `delete(id)`. `save` checks name (1–50 characters), dates, rate > 0, then
  locks the room type row (`select … for update`) and rejects an overlap with another seasonal rate of the same
  room type (`start_date <= other.end_date and end_date >= other.start_date`, its own id excluded). Errors are
  `BusinessRuleException`s. The foreign key to `room_type` is `on delete cascade`, so deleting an unused room
  type deletes its seasonal rates (edge case) without code in `RoomTypeService`.
  New view `room/SeasonalRateView`, route `manager/seasonal-rates`, `@RolesAllowed("MANAGER")`,
  `@Menu(title = "Seasonal rates", order = 52.5)` (between Room types 52 and Staff 53), built like
  `RoomTypeView`: grid, form, Save / Delete / New seasonal rate.
- **Rationale**: Locking the room type serializes two Managers saving overlapping rates, as the room lock does
  for bookings (001 research R3). Past dates are allowed (clarification), so there is no check against today.
- **Alternatives considered**: A unique/exclusion constraint for overlaps: H2 has no exclusion constraints.
  Seasonal rates as a tab inside `RoomTypeView`: a crowded view and harder to test.

## R6: Price per night text

- **Decision**: `static String NightRate.pricePerNight(List<NightRate>)` groups consecutive nights with the same
  rate in date order: `1 night × 100.00, 2 nights × 150.00`. Money is formatted with `toPlainString()`, as the
  total price is today. Used by `MyReservationView` (via `GuestReservation.nights`) and by a new "Price per
  night" column in the found-reservations grid of `ReservationView` (via `FoundReservation.nights`).
  `ReservationService.find` reads the night rows of the found reservations with one extra query.
- **Rationale**: FR-005 needs the same text in two places; a pure static method is unit-tested without Spring.
  Reception has no separate details page: the found-reservations grid is where 002 put the online cancellation
  time, so the price per night goes there too.
- **Alternatives considered**: A details dialog for reception: new UI that the spec does not ask for.

## R7: Revenue from night rows

- **Decision**: `ReportService.monthlyReport` counts and sums `reservation_night` rows joined with
  `reservation` (status checked in or checked out) whose `night_date` lies in the month: `count(*)` = occupied
  room-nights, `sum(nightly_rate)` = revenue.
- **Rationale**: A stay across a season boundary or a month end is split night by night (FR-009, US4) with one
  query instead of the current loop.

## R8: Labels and existing tests

- **Decision**: `RoomTypeView` calls the room type's rate "Standard nightly rate" (grid header, field, required
  message); `RoomTypeService` says "Standard nightly rate must be greater than 0.". The column stays
  `room_type.nightly_rate` (no rename, no regenerated references). The "Nightly rate" column is removed from the
  search grids of `ReservationView` and `GuestBookingView` (spec Assumptions).
- **Affected tests**: `RoomTypeViewTest`, `RoomTypeServiceTest`, `ManagerIT` (label and message);
  `ReservationServiceTest` (asserts `getNightlyRate()` → night rows; repricing cases follow R4);
  `ReportServiceTest.stay` (inserts night rows instead of `NIGHTLY_RATE`); `CancellationRuleMigrationTest`
  (its insert after the full migration no longer names `nightly_rate`); `AvailabilityServiceTest`,
  `ReservationViewTest`, `GuestBookingViewTest` (columns), `GuestReservationServiceTest`, `MyReservationViewTest`
  (price per night).
