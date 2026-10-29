---
stage: test
bolt: 002-reservation
created: 2026-10-29T11:05:11Z
---

## Test Report: reservation

### Summary

- **Unit Tests**: 2/2 passed (`StayPeriodTest`); coverage not measured (no coverage plugin in the build, JaCoCo not available offline)
- **Integration Tests**: 34/34 passed
  - `ReservationServiceTest` 17 (search, create, find against H2)
  - `ReservationConcurrencyTest` 1 (8 parallel bookings, same room and nights)
  - `AvailabilityViewTest` 10, `ReservationListViewTest` 6 (Browserless, real services and H2)
  - `PhoneBookingIT` 1 (Playwright: search → book → booking number)
- **Security Tests**: covered inside the above: booking number format/alphabet, no guest data in logs by design (not asserted)
- **Performance Tests**: none; the phone flow IT completes in seconds once the frontend is built
- **Whole build**: `./mvnw verify` - 65 surefire + 1 failsafe tests, BUILD SUCCESS (includes the 29 room catalog tests, unchanged)

### Acceptance Criteria Validation

Story 001 - search availability
- ✅ **Free in-service rooms with capacity ≥ guests, with type, capacity, rate and total**: `searchListsFreeInServiceRoomsWithEnoughCapacityAndTotalPrice`, `searchShowsFreeRoomsWithTotalPrice`
- ✅ **Overlapping confirmed reservation excludes the room**: `searchExcludesRoomWithOverlappingConfirmedReservation`
- ✅ **Back-to-back allowed**: `searchAllowsBackToBackStays`
- ✅ **Cancelled reservation does not block**: `searchIgnoresCancelledReservations`
- ✅ **Past arrival / departure not after arrival → date error, no results**: `rejectsArrivalInThePast`, `rejectsDepartureNotAfterArrival`, `StayPeriodTest`, `searchRejectsPastArrivalAndZeroGuests`
- ✅ **No match → "No rooms available for these dates"**: `showsMessageWhenNoRoomMatches`
- ✅ **Edge cases**: 0 guests (`rejectsZeroGuests`), 1-night price (`searchPricesOneNightAtTheNightlyRate`), out-of-service excluded (`searchExcludesOutOfServiceRooms`)

Story 002 - create reservation
- ✅ **CONFIRMED reservation stored, total = rate × nights, booking number shown**: `createStoresConfirmedReservationPricedByNights`, `booksRoomAndShowsBookingNumber`, `PhoneBookingIT`
- ✅ **Missing name/email/phone or invalid email → field error, nothing stored**: `requiresNameEmailAndPhone`, `rejectsInvalidEmail`
- ✅ **More guests than capacity → capacity error, nothing stored**: `createRejectsMoreGuestsThanCapacity`, `rejectsMoreGuestsThanRoomCapacity`
- ✅ **Booked by someone else since the search → "Room is no longer available for these dates"**: `createRejectsRoomBookedForOverlappingNightsSinceTheSearch`, `reportsRoomBookedByOthersSinceTheSearch`
- ✅ **Two simultaneous bookings → exactly one succeeds**: `exactlyOneOfParallelBookingsForTheSameRoomAndNightsSucceeds` (8 threads). Verified to fail 3/3 runs when the room lock is removed.
- ✅ **Rate change later leaves total unchanged**: `totalPriceStaysWhenRateChangesLater`
- ✅ **Edge cases**: room out of service after search (`createRejectsRoomSetOutOfServiceSinceTheSearch`), email case kept (`createStoresConfirmedReservationPricedByNights`)

Story 003 - find reservations
- ✅ **Default: confirmed, departure today or later, sorted by arrival, all columns**: `listShowsUpcomingConfirmedReservationsSortedByArrival`, `listsUpcomingConfirmedReservationsSortedByArrival`
- ✅ **Booking number → exactly that reservation, any status (lowercase matched)**: `bookingNumberFindsExactlyThatReservationInAnyStatusIgnoringCase`, `filtersByBookingNumberInAnyStatus`
- ✅ **Partial name "mül", case-insensitive**: `guestNameMatchesPartOfTheNameIgnoringCase`, `filtersByPartOfGuestNameIgnoringCase`
- ✅ **Date range → overlapping stays**: `dateRangeFindsStaysOverlappingTheRange`, `filtersByStaysOverlappingDateRange`
- ✅ **Show cancelled includes cancelled**: `showCancelledIncludesCancelledReservations` (service and view)
- ✅ **No match → "No reservations found"**: `showsMessageWhenNothingMatches`

### Issues Found

1. **H2 returns a stale `EXISTS` result within one transaction** (fixed): after an uncommitted insert, an identical `select exists (...)` in the same transaction still returned false, so a second booking of the same room and dates inside one transaction was accepted. Committed transactions were not affected (verified), so the running app was safe, but the check was fragile. All existence checks in `ReservationService` and `RoomCatalogService` now use `count(*) > 0`.
2. **Empty email error message** (fixed): `EmailField`'s built-in format check runs before Binder validators and had no message; it now carries "Enter a valid email address" and the duplicate validator was removed.
3. Test-only: Browserless queries skip invisible components; the "message hidden" check now uses `exists()`.

### Recommendations

- Add JaCoCo to the build if a coverage number is wanted (the pom already has `jacoco.version`).
- `PhoneBookingIT` takes ~2 minutes on a cold machine (frontend dev bundle build); later runs ~7 s.
- Bolt 003 (change reservation) must reuse the room lock and the overlap condition (ADR-001).
