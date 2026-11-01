# Tasks

## 1. Data model

- [ ] 1.1 Add `src/main/resources/db/migration/V7__add_seasonal_rates.sql` with the tables `seasonal_rate` and
  `reservation_night`, the index and the copy of existing reservations into night rows (design D1); keep
  `reservation.nightly_rate`; verify `./mvnw compile` succeeds and generates `SEASONAL_RATE` and `RESERVATION_NIGHT`
- [ ] 1.2 Add `src/test/java/ch/martinelli/edu/hrs/core/MigrationV7Test`: Flyway on a fresh in-memory H2 database up
  to target 6, insert a room type, a room and a reservation from 10 to 13 March 2030 at 140.00, migrate to 7, assert
  3 `reservation_night` rows for 10, 11 and 12 March at 140.00; verify `./mvnw test -Dtest=MigrationV7Test` is green
- [ ] 1.3 Change `ReservationTestData.insert` to also insert one `reservation_night` row per night at 140.00 and add
  an overload with a rate per night; verify `./mvnw test` stays green

## 2. Rate of each night

- [ ] 2.1 In `AvailabilityService` add record `NightRate`, `nightRates(roomTypeId, arrival, departure)` and static
  `totalPrice(List<NightRate>)` (design D2); verify with 2.3
- [ ] 2.2 Change `findAvailableRooms` to calculate the total price from `nightRates` once per room type and remove
  `nightlyRate` from `AvailableRoom`; remove the "Nightly rate" column from the `ReservationDialog` rooms grid and the
  "Nightly rate" line from the `BookingView` room cards; verify with 2.3 and 2.4
- [ ] 2.3 In `AvailabilityServiceTest` cover: night inside a season, night outside every season, season cheaper than
  the base rate, season of another room type ignored, stay 18–21 December 2030 across the start of "Christmas"
  (20 December, 180.00) costs 460.00 for "Double" at base 140.00, last night of a season is covered; verify
  `./mvnw test -Dtest=AvailabilityServiceTest` is green
- [ ] 2.4 Update `ReservationViewTest` and `BookingViewTest` (Browserless): search results show no nightly rate; the
  guest card for "Double" from 18 to 21 December 2030 with "Christmas" shows total CHF 460.00; verify
  `./mvnw test -Dtest=ReservationViewTest,BookingViewTest` is green

## 3. Night rates saved with the reservation

- [ ] 3.1 In `ReservationService.create` insert the night rows from `nightRates` after the reservation (design D3);
  keep setting `reservation.nightly_rate` to the base rate until 6.1; verify with 3.4
- [ ] 3.2 In `ReservationService.change` keep saved rates for nights the reservation already had, price added
  nights with `nightRates`, re-price every night when the room type changes, replace the rows; add
  `nights(reservationId)`; verify with 3.4
- [ ] 3.3 In `FrontDeskService.checkOut` delete the night rows on or after the new departure date (design D4);
  verify in `FrontDeskServiceTest` that an early check-out on 12 March of a 10–14 March stay leaves 2 night rows and
  `./mvnw test -Dtest=FrontDeskServiceTest` is green
- [ ] 3.4 In `ReservationServiceTest` replace the `getNightlyRate` assertions with night rows and cover: stay across
  the start of a season saves 140.00, 140.00, 180.00; base rate change after booking keeps the rows; seasonal rate
  change and deletion after booking keep the rows; moved to "Family" re-prices every night at 220.00; extended into a
  season keeps 130.00 for the old nights and adds 180.00 for the new ones; shortened stay drops the removed nights;
  verify `./mvnw test -Dtest=ReservationServiceTest` is green

## 4. Price per night and totals in views and email

- [ ] 4.1 Add static `ReservationView.pricePerNight(List<NightRate>)` grouping consecutive nights with the same rate,
  "1 night" singular (design D7); verify with 4.4
- [ ] 4.2 In `ReservationDialog` details replace "Nightly rate" with "Price per night" and compute "Total price" from
  `ReservationService.nights`; in `MyReservationView` add "Price per night" and compute the total the same way;
  verify with 4.4
- [ ] 4.3 In `BookingConfirmationEmail` and the `BookingView` confirmation compute the total price from the saved
  nights; verify the existing email test in `BookingViewTest` still finds the total price
- [ ] 4.4 Browserless tests: in `ReservationViewTest` the details of an 18–21 December 2030 reservation with rates
  140.00, 140.00, 180.00 show "2 nights × CHF 140.00, 1 night × CHF 180.00" and "CHF 460.00"; in
  `MyReservationViewTest` the guest sees the same; a unit test for `pricePerNight` covers one night, one rate and
  low-high-low runs; verify `./mvnw test -Dtest=ReservationViewTest,MyReservationViewTest` is green

## 5. Revenue from night rows

- [ ] 5.1 Rewrite `ReportService.yearReport` to count room-nights and sum revenue from `reservation_night` joined
  with `reservation` (Checked in, Checked out), grouped by month (design D5); verify with 5.2
- [ ] 5.2 In `ReportServiceTest`, `ReportViewTest` and `ReportIT` replace updates of `RESERVATION.NIGHTLY_RATE` with
  night rates from `ReservationTestData`; add the scenario 31 December 2030 at 180.00 and 1–2 January 2031 at 180.00
  and 140.00 giving 180.00 for December 2030 and 320.00 for January 2031; verify
  `./mvnw test -Dtest=ReportServiceTest,ReportViewTest` is green

## 6. Drop the single reservation rate

- [ ] 6.1 Add `src/main/resources/db/migration/V8__drop_reservation_nightly_rate.sql` dropping
  `reservation.nightly_rate`; remove the remaining reads and writes (`ReservationService.create`, `change`,
  `ReservationTestData`) and the old `totalPrice(rate, arrival, departure)`; verify `./mvnw compile` succeeds, `grep
  -rn "NIGHTLY_RATE\|getNightlyRate\|setNightlyRate" src` finds only `ROOM_TYPE` and `SEASONAL_RATE`/`RESERVATION_NIGHT`
  uses, and `./mvnw test` is green

## 7. Base rate label and seasonal rates page

- [ ] 7.1 In `RoomTypeView` and `RoomView` rename the label and column to "Base rate" with the messages "Base rate is
  required", "Base rate must be greater than zero", "Base rate has at most two decimals" (design D7); update
  `RoomTypeViewTest` and `RoomManagementIT` selectors and messages; verify `./mvnw test -Dtest=RoomTypeViewTest` is
  green
- [ ] 7.2 In `RoomService` add `findAllSeasonalRates`, `saveSeasonalRate` (last night not before first night, room
  type row locked, overlap check excluding itself) and `deleteSeasonalRate` with `SeasonalRateException` (design D6);
  in `RoomServiceTest` cover: created, last night before first night, overlap message
  `The dates overlap the seasonal rate "Christmas" of this room type`, same dates for another room type allowed,
  change of itself not an overlap, changed dates overlap, deleted; verify `./mvnw test -Dtest=RoomServiceTest` is
  green
- [ ] 7.3 Add `room/ui/SeasonalRateView` at route `seasonal-rates` for the Manager role and the menu entry "Seasonal
  rates" in `StaffLayout` (design D6); verify with 7.4
- [ ] 7.4 Add `SeasonalRateViewTest` (Browserless): list sorted by room type and first night; create "Christmas" for
  "Double"; each missing or invalid field marked; last night before first night message; overlap message shown;
  change the rate; delete; Receptionist gets access denied; extend `StaffLayoutTest` for the menu entry shown only to
  the Manager; verify `./mvnw test -Dtest=SeasonalRateViewTest,StaffLayoutTest` is green

## 8. Integration

- [ ] 8.1 Add `SeasonalRatesIT` (Playwright): the manager creates the seasonal rate "Christmas" for "Double" from
  20 December to 6 January of next year at 180.00; a guest then searches 18 to 21 December and sees "Double" at the
  total price of 2 × base rate + 180.00; books it; finds it on the lookup page and sees the price per night; verify
  the test passes in `./mvnw verify`
- [ ] 8.2 Run `./mvnw verify`; verify it ends with BUILD SUCCESS and no failing tests

## Workflow follow-up

- Run `/opsx:verify add-seasonal-rates` and fix findings.
- Archive with `/opsx:archive add-seasonal-rates` after review; the delta specs then merge into
  `openspec/specs/seasonal-rates` (new), `room-management`, `reservations`, `availability`, `online-booking` and
  `reports`.
