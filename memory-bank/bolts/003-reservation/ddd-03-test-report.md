---
stage: test
bolt: 003-reservation
created: 2026-10-29T11:56:52Z
---

## Test Report: reservation (lifecycle)

### Summary

- **Unit Tests**: rule methods covered through the service tests below; coverage not measured (no coverage plugin in the build)
- **Integration Tests**: 49 new, all passed
  - `ReservationLifecycleTest` 16 (change, cancel, anonymize against H2)
  - `RoomCatalogServiceTest` +3 (room protection)
  - `LifecycleConcurrencyTest` 20 (2 races × 10 repetitions)
  - `ReservationListViewTest` +8, `RoomViewTest` +2 (Browserless)
- **Security Tests**: anonymization replaces all three personal fields and keeps the rest; state rules enforced in the service, not only by disabled buttons
- **Performance Tests**: none needed at this volume
- **Whole build**: `./mvnw verify` - 114 surefire + 1 failsafe (`PhoneBookingIT`), BUILD SUCCESS

**Lock mutation check**: removing the room lock in `RoomCatalogService.saveRoom` made 10/20 concurrency runs fail; removing the room lock in `ReservationService.lockBookableRoom` made 12/21 fail. Both locks restored.

### Acceptance Criteria Validation

Story 004 - change reservation
- ✅ **Dates changed, price recalculated with current rate, booking number kept**: `changingDatesRecalculatesPriceWithCurrentRateAndKeepsBookingNumber`, `changesDatesInTheDialog`
- ✅ **Extending does not conflict with itself**: `extendingDoesNotConflictWithItself`
- ✅ **Overlap with another reservation → "Room is not available for these dates", nothing changes**: `changeRejectsOverlapWithAnotherReservation`, `reportsUnavailableRoomAndKeepsTheDialogOpen`
- ✅ **Too small or out-of-service room → error, nothing changes**: `changeRejectsRoomThatIsTooSmall`, `changeRejectsRoomOutOfService`, `showsCapacityErrorInTheDialog`
- ✅ **Cancelled or departed → change not possible**: `changeIsNotPossibleForCancelledOrDepartedReservations`, `actionsFollowTheSelectedReservation`
- ✅ **Only phone changed → price not recalculated**: `changingOnlyGuestDataKeepsThePrice`
- ✅ **Edge: staying guest moves departure, not arrival into the past**: `stayingGuestMayMoveDepartureButNotArrivalIntoThePast`; departure today still changeable (approved decision): `reservationDepartingTodayCanStillBeExtended`
- ✅ **Edge: concurrent change and new booking → exactly one succeeds**: `changeAndNewBookingForTheSameNightsNeverBothSucceed`

Story 005 - cancel reservation
- ✅ **Cancel after confirming → CANCELLED, not deleted**: `cancelKeepsReservationAndFreesTheNights`, `cancelsAfterYes`
- ✅ **"No" → nothing changes**: `keepsReservationWhenCancelIsAnsweredNo`
- ✅ **Cancelled nights free in search**: `cancelKeepsReservationAndFreesTheNights`
- ✅ **Cancelled → no cancel or change**: `cancelledReservationCannotBeCancelledOrChangedAgain`, `actionsFollowTheSelectedReservation`
- ✅ **Edge: cancel during the stay allowed**: `receptionMayCancelDuringTheStay`

Story 006 - protect rooms with reservations
- ✅ **Room with any reservation cannot be deleted, message shown**: `roomWithAnyReservationCannotBeDeleted`, `refusesToDeleteRoomWithReservations`
- ✅ **Future confirmed reservations block out of service, booking numbers listed**: `roomWithFutureConfirmedReservationsCannotGoOutOfServiceAndMessageListsThem`, `refusesOutOfServiceWithFutureReservationsAndListsThem`
- ✅ **Only cancelled / past reservations → out of service succeeds; departs today counts as not future**: `roomWithOnlyCancelledPastOrTodayDepartingReservationsCanGoOutOfService`
- ✅ **Concurrent booking and out of service never both succeed**: `bookingAndOutOfServiceNeverBothSucceed`

Story 007 - anonymize guest data
- ✅ **Cancelled or departed → name, email, phone "Anonymized", rest kept**: `anonymizeReplacesPersonalDataAndKeepsTheRest`, `departedConfirmedReservationCanBeAnonymized`, `anonymizesAfterYesAndShowsItAsAnonymized`
- ✅ **Confirmed departing today or later → not possible**: `confirmedReservationDepartingTodayOrLaterCannotBeAnonymized`, `actionsFollowTheSelectedReservation`
- ✅ **Anonymized → shown as anonymized, no second anonymize or change**: `anonymizedReservationCannotBeAnonymizedOrChangedAgain`, `anonymizesAfterYesAndShowsItAsAnonymized`
- ✅ **"No" → nothing changes**: `keepsGuestDataWhenAnonymizeIsAnsweredNo`

### Issues Found

None in production code during this stage.

### Recommendations

- Decide whether anonymized CONFIRMED (departed) reservations should still be cancellable; currently allowed because the rule is "any CONFIRMED".
- `ReservationTestData` is the shared test setup for new tests; `ReservationServiceTest` from bolt 002 still has its own helpers and could move to it.
- Add JaCoCo to the build if a coverage number is wanted.
