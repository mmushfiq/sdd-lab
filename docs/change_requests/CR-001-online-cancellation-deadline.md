# CR-001: Online Cancellation Deadline 48 Hours

**Requested by:** Revenue Manager
**Received:** 2026-10-26
**Status:** Specified, waiting for approval

## Request

> Too many guests cancel online the day before they arrive, and we can't resell the room in time. Move the online
> cancellation deadline from 24 hours to 48 hours before check-in. Guests who already booked were promised 24 hours,
> so their reservations keep the old deadline. Also show guests the exact last cancellation time, on the lookup page
> and in the confirmation email.

## Parts

| Part | Change                                                         | Kind              |
|------|----------------------------------------------------------------|-------------------|
| a    | Online cancellation deadline 48 h instead of 24 h              | Rule changed      |
| b    | Reservations recorded before the change keep 24 h              | New data          |
| c    | Show the deadline on the lookup page and in confirmation emails | Behavior changed |

## Decisions (stakeholder answers, 2026-10-26)

1. Changing the arrival date (UC-005) sets the deadline again with the 48-hour rule, also for a reservation that had
   24 hours → UC-005 BR-007.
2. When the deadline has already passed (late booking), the guest is told that online cancellation is no longer
   possible and that reception can still cancel → UC-003 BR-006.
3. The change confirmation email (UC-005) also shows the deadline → UC-005 step 8.

## Assumptions (confirm on review)

- "Already booked" means recorded before the change goes live; no fixed calendar date is needed because the deadline
  is stored per reservation.
- Reservations made at reception (UC-004) get the 48-hour deadline as well; the deadline is about online cancellation,
  not about the channel.
- Changing only the departure date, the room type or the number of guests keeps the deadline (UC-005 BR-007).
- The deadline is shown as the moment from which online cancellation is no longer possible ("before 8 May 2031, 14:00").
- The confirmation page right after reserving (UC-001 step 11) is unchanged; the request names only the lookup page
  and the email.

## Impact

| Artifact                 | Affected | Change                                                                           |
|--------------------------|----------|----------------------------------------------------------------------------------|
| requirements.md          | yes      | FR-003, FR-004, FR-005 text; FR-001, FR-004, FR-006, FR-007, NFR-001, NFR-003, NFR-008, NFR-011, NFR-012 back to Open |
| glossary.md              | yes      | Cancellation; new term Online Cancellation Deadline                              |
| entity_model.md          | yes      | Reservation.online_cancellation_deadline (Not Null) and its constraint           |
| UC-001 Reserve Room Online | yes    | Step 9, A7, postcondition, BR-011: deadline recorded and in the email            |
| UC-002 View Own Reservation | yes   | Step 4 shows the deadline; Implemented → Approved                                |
| UC-003 Cancel Own Reservation | yes | Step 4, A5 trigger, BR-001 rewritten, new BR-006                                 |
| UC-004 Create Reservation | yes     | Steps 10 and 11; Implemented → Approved                                          |
| UC-005 Change Reservation | yes     | Steps 7 and 8, postcondition, new BR-007; Implemented → Approved                 |
| UC-006 Cancel Reservation | no      | Refers to UC-003 BR-001 by id                                                    |
| other use cases          | no       | Searched for the old rule; no reference                                          |

## Handoff to Construction

Run in this order once the spec change is approved:

1. `/flyway-migration`: new migration (next version after V006) that adds `reservation.online_cancellation_deadline`.
   Fill existing rows with the old rule (arrival date at 14:00 minus 24 hours), then make the column Not Null.
   Never edit V001–V006: production has already run them.
   The test data (`src/test/resources/db/migration/V100`–`V120`) and the dev seed (`src/main/resources/db/seed/V900`)
   insert reservations and must set the new column.
2. `/implement UC-003`, `UC-002`, `UC-001`, `UC-004`, `UC-005`.
3. `/browserless-test` for the same use cases. Existing UC-003 tests for 24 h (13:59 / 14:00 on the day before arrival)
   must follow the new BR-001; add tests for a reservation with a stored 24-hour deadline, for the shown deadline
   (UC-003 BR-006), and for UC-005 BR-007.
4. `/coverage-check` for the same use cases, then move the statuses forward.

## Semantic Review Notes (advisory)

- info: UC-003 step 4 shows the deadline without saying "for a Confirmed reservation"; BR-006 limits it.
- info: UC-003 BR-001 names the cutoff as "before the 48-hour deadline took effect" and not as a date; it is testable
  through the stored deadline.
