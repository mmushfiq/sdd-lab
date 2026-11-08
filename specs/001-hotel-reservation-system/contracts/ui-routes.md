# UI Contract: Routes and Access

The application exposes only Vaadin Flow views (no REST API). This is the contract tests and navigation rely
on.

| Route | View | Access | User stories | What it offers |
|---|---|---|---|---|
| `` (root) | `GuestBookingView` | anonymous | 3 | search by dates and guests, book a room, show reservation number |
| `my-reservation` | `MyReservationView` | anonymous | 7 | look up by reservation number + email, cancel until the online cancellation deadline (see 002) |
| `login` | `LoginView` | anonymous | all staff | staff login; after login go to the role's first view |
| `reception/reservations` | `ReservationView` | RECEPTIONIST | 2, 6 | search availability, create, find, change, cancel reservations |
| `reception/front-desk` | `FrontDeskView` | RECEPTIONIST | 4 | today's arrivals and departures, check-in, check-out, room overview |
| `housekeeping` | `CleaningListView` | HOUSEKEEPING | 5 | dirty rooms, mark cleaned (tablet layout) |
| `manager/room-types` | `RoomTypeView` | MANAGER | 1 | create, change, delete room types |
| `manager/rooms` | `RoomView` | MANAGER | 1 | create, change, delete rooms |
| `manager/seasonal-rates` | `SeasonalRateView` | MANAGER | 003 US1 | create, change, delete seasonal rates |
| `manager/staff` | `StaffUserView` | MANAGER | 1 | create users, change role, reset password, deactivate |
| `manager/reports` | `ReportView` | MANAGER | 8 | month picker, occupancy %, revenue |

Rules:

- A staff route opened without login redirects to `login` (US 3 scenario 5).
- A staff route opened with the wrong role is denied (US 1/5/8 access scenarios).
- The side menu shows only routes the current user may open; a logout button is in the layout.
- First view after login: Receptionist → `reception/front-desk`, Manager → `manager/rooms`,
  Housekeeping → `housekeeping`.
- User-facing errors (double booking, dirty room, rule violations) are shown as notifications or field
  errors with the messages named in the spec's acceptance scenarios.
