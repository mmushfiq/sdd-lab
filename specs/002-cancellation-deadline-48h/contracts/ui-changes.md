# UI Contract Changes

Routes and access are unchanged (see `specs/001-hotel-reservation-system/contracts/ui-routes.md`). Only
what the views show changes.

| Route | View | Change |
|---|---|---|
| `my-reservation` | `MyReservationView` | Booked, deadline not passed: paragraph id `cancellation-deadline` with `text()` and the "Cancel reservation" button. Booked, deadline passed: paragraph id `cancellation-deadline` with `passedText()`, no button. Cancelled, checked in, checked out: no paragraph, no button. |
| `my-reservation` | `MyReservationView` | Cancel rejected by the service (deadline passed between lookup and confirm): error notification with `passedText()`. |
| `` (root) | `GuestBookingView` | After a successful online booking, sends the confirmation email (`confirmation-email.md`) before showing the confirmation. Confirmation text unchanged; a mail failure is not shown to the guest. |
| `reception/reservations` | `ReservationView` | Found-reservations grid gets a column "Online cancellation" showing `text()` for booked reservations whose deadline has not passed, `passedText()` without the last sentence for passed ones, empty otherwise. |

Texts (`CancellationDeadline`, data-model.md):

- `Free online cancellation until 10 March 2030, 15:00 (Zurich time)`
- `Online cancellation was possible until 10 March 2030, 15:00 (Zurich time). Please call reception to cancel.`

Date format `d MMMM yyyy, HH:mm`, English month names, Europe/Zurich.
