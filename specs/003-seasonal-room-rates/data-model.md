# Data Model: Seasonal Room Rates

Changes to the data model of 001 and 002 only (see `specs/001-hotel-reservation-system/data-model.md`).

## Room Type (changed meaning)

`room_type.nightly_rate` is now the **standard nightly rate**: the rate of every night that no seasonal rate
covers (FR-001). Column name and rules unchanged; only labels change (research R8).

## Seasonal Rate (new)

| Field | Type | Rule |
|---|---|---|
| `id` | bigint, PK | from `seasonal_rate_seq` |
| `room_type_id` | bigint, not null | references `room_type (id)` on delete cascade |
| `name` | varchar(50), not null | required, e.g. "Christmas" |
| `start_date` | date, not null | first night covered |
| `end_date` | date, not null | last night covered (inclusive); `check (end_date >= start_date)` |
| `nightly_rate` | decimal(10, 2), not null | `check (nightly_rate > 0)` |

- Seasonal rates of the same room type MUST NOT overlap: checked in `SeasonalRateService.save` under a lock on
  the room type row (research R5). The message names the overlapped one:
  `Overlaps the seasonal rate "Summer" (1 July 2027 – 31 August 2027).`
- Other messages: `Name is required.`, `Name can have at most 50 characters.`, `Start date and end date are required.`,
  `End date must not be before start date.`, `Nightly rate must be greater than 0.`, `Room type is required.`
- Past dates are allowed. Index `seasonal_rate_room_type_dates (room_type_id, start_date)`.

## Reservation Night (new)

| Field | Type | Rule |
|---|---|---|
| `reservation_id` | bigint, not null | references `reservation (id)` |
| `night_date` | date, not null | the date the night starts on; from `arrival_date` to `departure_date − 1` |
| `nightly_rate` | decimal(10, 2), not null | `check (nightly_rate > 0)`; frozen at booking (FR-006) |

Primary key `(reservation_id, night_date)`. Every reservation has exactly one row per night of its stay, and
`reservation.total_price` = sum of its rows' `nightly_rate`.

## Reservation (changed)

- `nightly_rate` is removed (V6, research R2). Its value lives on in the night rows.
- `total_price` unchanged in meaning: sum of the night rates.

## Rate of a night (derived, FR-004)

`rate(roomType, night)` = `nightly_rate` of the seasonal rate of that room type with
`start_date <= night <= end_date`, otherwise the room type's standard nightly rate.

Example: standard 100, seasonal "Summer" 150 from 1 July to 31 August.

| Stay | Nights | Total |
|---|---|---|
| 10–13 July | 10, 11, 12 July × 150 | 450 |
| 30 June – 2 July | 30 June × 100, 1 July × 150 | 250 |
| 30 June – 3 July | 30 June × 100, 1–2 July × 150 | 400, shown as `1 night × 100.00, 2 nights × 150.00` |

## Change of a booked reservation (FR-007, research R4)

| Change | Night rates |
|---|---|
| Same room type, dates changed | nights already booked keep their saved rate; new nights get today's rate; removed nights are dropped |
| Other room of the same type, same dates | unchanged |
| Other room type | every night gets today's rate of the new room type |

Example: booked 10–12 July at 120 (before "Summer" 150 existed), extended to 13 July → 10 and 11 July keep 120,
12 July gets 150, total 390.

## Migration

- `V5__seasonal_rates.sql`: sequence `seasonal_rate_seq`; tables `seasonal_rate`, `reservation_night`; one night
  row per night of every existing reservation at its `nightly_rate` (FR-008); `reservation.nightly_rate` becomes
  nullable, so new bookings no longer write it.
- `V6__drop_reservation_nightly_rate.sql`: drops `reservation.nightly_rate` after the code no longer uses it.

## State transitions

Unchanged. Night rows belong to the reservation in every status; cancelled, booked and checked-in nights are not
revenue (only checked in and checked out count, 001 FR-021).
