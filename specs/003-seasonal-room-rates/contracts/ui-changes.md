# UI Contract Changes

Routes of 001 are unchanged (see `specs/001-hotel-reservation-system/contracts/ui-routes.md`); one route is new.

## New route

| Route | View | Role | Spec | Content |
|---|---|---|---|---|
| `manager/seasonal-rates` | `SeasonalRateView` | MANAGER | US1, FR-002, FR-003, FR-010 | Menu "Seasonal rates" after "Room types". Grid id `seasonal-rates`: Room type, Name, Start date, End date, Nightly rate, sorted by room type and start date. Form: Room type (combo box), Name (max 50), Start date, End date, Nightly rate. Buttons: Save, Delete (enabled when a row is selected), New seasonal rate. Rejections from the service are shown as error notifications with the service message (data-model.md). Receptionist and Housekeeping get access denied, like the other manager routes. |

## Changed views

| Route | View | Change |
|---|---|---|
| `manager/room-types` | `RoomTypeView` | "Nightly rate" becomes "Standard nightly rate" (grid header, field label, "Standard nightly rate is required"). |
| `` (root) | `GuestBookingView` | Search grid: column "Nightly rate" removed; "Total price" is the sum of the night rates (FR-005). |
| `reception/reservations` | `ReservationView` | Search grid: column "Nightly rate" removed. Found-reservations grid: new column "Price per night" with the grouped text, before "Total price". |
| `my-reservation` | `MyReservationView` | New form item "Price per night" with the grouped text, before "Total price". |
| `manager/reports` | `ReportView` | No visible change; revenue uses the night rates (FR-009). |

The booking confirmation (screen and email, 002 `contracts/confirmation-email.md`) is unchanged: it shows the
total price, which is now the sum of the night rates.

## Price per night text (`NightRate.pricePerNight`)

Consecutive nights with the same rate in date order, joined with ", ": `<n> night(s) × <rate>`.

- `1 night × 100.00, 2 nights × 150.00`
- `3 nights × 120.00`
- Non-consecutive nights at the same rate stay separate groups:
  `1 night × 150.00, 1 night × 100.00, 1 night × 150.00`

Rates are formatted like the total price today (`toPlainString()`, two decimals).
