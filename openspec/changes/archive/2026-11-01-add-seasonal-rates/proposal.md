# Proposal

## Why

Every room type has one nightly rate for the whole year. In summer and around Christmas the hotel is fully booked
weeks ahead and could charge more; in November rooms stay empty because the price is too high. Today the manager
changes the rate by hand before each season and sometimes forgets to change it back. The manager needs rates that
switch by date on their own.

## What Changes

- The rate of a room type becomes its **base rate**: the rate for every night that no seasonal rate covers.
- New: the manager maintains **seasonal rates** on a new "Seasonal rates" page. A seasonal rate belongs to one room
  type and has a name (e.g. "Christmas"), a first night, a last night and a nightly rate. It may be higher or lower
  than the base rate. Seasonal rates of the same room type must not overlap. They can be created, changed and
  deleted. Seasons use fixed dates and are entered again for each year.
- **Each night of a stay is priced on its own**: the seasonal rate of the room type covering that night, otherwise
  the base rate. A stay that crosses into a season costs the low rate for the nights before and the high rate for
  the nights inside it.
- **BREAKING**: a reservation no longer has one nightly rate. It keeps the rate of **each night**, as it was when the
  reservation was made. Its total price is the sum of these night rates. Later changes of base or seasonal rates do
  not change it.
- Changing a Booked reservation: nights it already had keep their saved rate, added nights get the rate that applies
  now, and moving it to another room type prices every night again at that room type's current rates.
- **BREAKING**: availability results for reception and guests no longer show a single nightly rate, only the total
  price for the stay.
- The reservation details for reception and for the guest show the price per night grouped by rate, e.g.
  "2 nights × CHF 140.00, 1 night × CHF 180.00", followed by the total price.
- Monthly revenue in the reports adds up the saved rate of each occupied night, so a stay across a season boundary
  counts the right amount in each month.

## Capabilities

### New Capabilities

- `seasonal-rates`: date ranges with their own nightly rate per room type, the rule that decides the rate of a
  night, and the manager's page to maintain them.

### Modified Capabilities

- `room-management`: the room type's nightly rate is now called the base rate; rooms and room types show the base
  rate (modified).
- `reservations`: a reservation keeps the rate of each night instead of one nightly rate; details show the price per
  night; the rate rules when changing a reservation (modified).
- `availability`: search results show the total price as the sum of the night rates, without a single nightly rate
  (modified).
- `online-booking`: guest search results show no single nightly rate; the guest's reservation shows the price per
  night (modified).
- `reports`: monthly revenue adds up the saved rate of each night (modified).

## Impact

- Database: new Flyway migration with the tables `seasonal_rate` and `reservation_night`; existing reservations get
  one night row per night at their current rate; the column `reservation.nightly_rate` is dropped.
- Code: `AvailabilityService` (rate of each night, total price), `ReservationService` (create and change save night
  rates), `FrontDeskService.checkOut` (early check-out removes the nights not stayed), `ReportService` (revenue from
  night rates), `RoomService` (seasonal rates), new `room/ui/SeasonalRateView`, `StaffLayout` (menu entry),
  `RoomTypeView` and `RoomView` (label "Base rate"), `ReservationDialog`, `BookingView`, `MyReservationView`,
  `BookingConfirmationEmail`.
- Tests: `ReservationTestData` and every test that sets or reads `reservation.nightly_rate` change to night rows;
  new tests for seasonal rates, prices across a season boundary and the revenue split.
