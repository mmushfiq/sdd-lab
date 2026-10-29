---
stage: test
bolt: 004-guest-booking
created: 2026-10-29T13:20:00Z
---

## Test Report: guest-booking

### Summary

- **Tests**: 37/37 new tests passed (36 unit + 1 Playwright); full `./mvnw verify`: 150 unit + 2 integration, 0 failures
- **Coverage**: no percentage target (coding standards); every acceptance criterion has at least one test

### Test Files

- [x] `src/test/java/ch/martinelli/edu/hrs/guestbooking/GuestBookingServiceTest.java` - 15 tests: grouping by room type, lowest-room assignment, skipping taken rooms, "no longer available" with nothing stored, lookup case/space handling, wrong number/email, anonymized reservations, deadline at 13:59:59 and 14:00:00, hotel time vs UTC, double cancel, wrong-email cancel
- [x] `src/test/java/ch/martinelli/edu/hrs/guestbooking/GuestSearchViewTest.java` - 10 tests: form and links, room-type grid without room numbers, fully booked type hidden, date validation, empty message, booking flow with confirmation and refreshed counts, field validation, room type taken since the search
- [x] `src/test/java/ch/martinelli/edu/hrs/guestbooking/GuestReservationViewTest.java` - 11 tests: lookup in any case, generic message for wrong number / wrong email / anonymized, required fields, cancel with Yes and No, no cancel action after the deadline or when cancelled, deadline passing while the page was open
- [x] `src/test/java/ch/martinelli/edu/hrs/guestbooking/GuestBookingIT.java` - Playwright: search → book → read booking number → look up → cancel in a real browser

### Acceptance Criteria Validation

Story 001 - search rooms online
- ✅ **Search form without login**: `opensWithSearchFormAndNoStaffLinks`
- ✅ **Room types with capacity, rate, total, free rooms; no room numbers**: `listsRoomTypesWithoutRoomNumbers`, `groupsFreeRoomsByTypeWithoutRoomNumbers`
- ✅ **Fully booked type not listed**: `hidesFullyBookedRoomType`, `omitsRoomTypeWithoutFreeRoom`
- ✅ **Date validation**: `rejectsArrivalInThePast`, `rejectsDepartureNotAfterArrival`
- ✅ **"No rooms available for these dates"**: `showsMessageWhenNothingIsFree`
- ✅ **No link to staff views**: `opensWithSearchFormAndNoStaffLinks` and `hasNoLinksToStaffViews` (only the other guest page is linked)

Story 002 - book room online
- ✅ **CONFIRMED reservation on the lowest free room, details and keep-number note shown**: `booksLowestRoomShowsConfirmationAndRefreshesCounts`, `booksTheFreeRoomWithTheLowestNumber`, `skipsRoomsOfTheTypeThatAreTaken`
- ✅ **Field errors, nothing stored**: `requiresNameEmailAndPhoneAndStoresNothing`, `rejectsInvalidEmail`
- ✅ **Last room taken after the search**: `reportsRoomTypeTakenSinceTheSearch`, `rejectsWhenNoRoomOfTheTypeIsLeftAndStoresNothing`
- ✅ **Privacy note**: asserted in `booksLowestRoomShowsConfirmationAndRefreshesCounts`
- ✅ **Listed for reception**: `onlineBookingAppearsInTheReceptionList`
- ⚠️ **Double click creates one reservation**: the button is set to disable on click (checked in code); Browserless tests have no browser-side click handling, so there is no automated test

Story 003 - view own reservation
- ✅ **Correct number + email, any case, trimmed**: `showsReservationForNumberAndEmailInAnyCase`, `findsReservationIgnoringCaseAndSurroundingSpaces`
- ✅ **Wrong number or email: generic message**: `showsGenericMessageForWrongEmail`, `showsSameGenericMessageForWrongNumber`, `findsNothingForWrongNumberOrWrongEmail`
- ✅ **Anonymized: same message**: `showsSameGenericMessageForAnonymizedReservation`, `anonymizedReservationIsNotFoundEvenWithTheOldEmailOrTheReplacement`
- ✅ **Empty field: required error**: `requiresBothFields`

Story 004 - cancel own reservation
- ✅ **Cancel before the deadline frees the room**: `cancelsAfterConfirmation`, `cancelsOneSecondBeforeTheDeadlineAndFreesTheRoom`
- ✅ **No cancel action and message after the deadline**: `offersNoCancelActionAfterTheDeadline`, `rejectsCancellationFromTheDeadlineOn`
- ✅ **No cancel action when CANCELLED**: `offersNoCancelActionForCancelledReservation`, `cancelledReservationCannotBeCancelledAgain`
- ✅ **"No" changes nothing**: `choosingNoChangesNothing`
- ✅ **Deadline re-checked at confirm time**: `rejectsConfirmAfterTheDeadlinePassedWhilePageWasOpen`
- ✅ **Deadline is hotel time**: `deadlineIsInHotelTimeNotUtc`

Unit success criteria
- ✅ **Playwright IT completes a guest booking flow**: `GuestBookingIT`
- ⚠️ **Under 3 minutes**: a manual check; not measured

### Issues Found

- None in the production code. During test writing, two test-data mistakes (9-character booking numbers) and two wrong assertions (dialog header read as child text; notification matching "CANCELLED") were fixed in the tests.

### Notes

- Deadline UI tests use arrivals of today (deadline passed) or 10 days out (far from the deadline), so they do not depend on the time of day; exact boundaries are tested in the service with `Clock.fixed`.
- In the Playwright test, dialog footer buttons are not found inside `vaadin-dialog-overlay`, so page-level locators are used.
- Not tested: concurrent guest bookings of the same room type (the locking is covered by the intent-001 concurrency tests).
