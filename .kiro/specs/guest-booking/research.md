# Research & Design Decisions

## Summary
- **Feature**: `guest-booking`
- **Discovery Scope**: Extension (light discovery). Public Vaadin views on top of the specified `ReservationService`
  (reservation-core) and the public entry page (foundation). The codebase is still a skeleton (`HrsBrowserlessTest`,
  `PlaywrightIT`, no routes, no migrations); foundation and reservation-core are specified but not implemented.
- **Key Findings**:
  - `ReservationService` already offers every operation the guest pages need: `findAvailableRooms`, `create`,
    `findByCode` (trimmed, case-insensitive), `cancel`. No upstream change is needed.
  - The reservation core deliberately leaves guest identification and the online cancel deadline to this spec
    (reservation-core D5, D9, Security Considerations: "guest-facing views use `findByCode` with their own proof
    check"). These two rules plus "e-mail required online" are the only new business rules, so one small service holds
    them.
  - The foundation design allows guest-booking to add a link on `PublicHomeView` (Out of Boundary: "guest-booking adds
    its own public routes and may add a link on the entry page"). `core` must not import feature packages, so the links
    use route strings, not view classes.

## Research Log

### Upstream contract (reservation-core)
- **Context**: Which calls, records and messages the guest pages may rely on.
- **Sources Consulted**: `.kiro/specs/reservation-core/requirements.md`, `design.md`, `tasks.md`.
- **Findings**:
  - Records `AvailableRoom(roomId, roomNumber, roomTypeName, capacity, nightlyRate, totalPrice)`,
    `NewReservation(roomId, arrivalDate, departureDate, numberOfGuests, GuestData)`, `GuestData(name, email, phone)`,
    `ReservationDetails(... guest)` with `nights()` and `totalPrice()`; `guest()` is null after erasure.
  - `ReservationException(Reason, message)` has a public constructor; reasons `NOT_FOUND`, `INVALID_INPUT`,
    `ROOM_NOT_AVAILABLE`, `TRANSITION_NOT_ALLOWED`. Messages never contain guest data.
  - Core accepts a reservation with phone only; e-mail format `text@text.text` is checked by core.
  - `search` exposes guest data and must be called only from staff views — guest views must not use it.
- **Implications**: Guest views call `findAvailableRooms` directly; booking, look-up and cancel go through
  `GuestBookingService`, which adds the online rules and delegates to `ReservationService`. No direct table access.

### Foundation seams
- **Context**: Public routes, access annotations, clock, test base classes.
- **Sources Consulted**: `.kiro/specs/foundation/design.md`, `tasks.md`; `src/test/java/.../core/ui/*`.
- **Findings**: Every view needs `@AnonymousAllowed` (default deny). `HrsBrowserlessTest` and `PlaywrightIT` get a
  fixed clock `2026-06-01T12:00:00Z` (foundation task 1.2). Public views are not inside `StaffLayout`.
- **Implications**: Guest views carry `@AnonymousAllowed` and no layout. The online cancel deadline cannot be reached
  with April 2027 reservations under the fixed clock, so the deadline rule takes "today" as a parameter in a
  package-private method that tests call with explicit dates; the public method reads the clock.

### Vaadin components
- **Context**: Component choice for search, form, confirmation and dialog.
- **Sources Consulted**: steering `vaadin-jooq.md`; Vaadin 25 Flow components already in use by foundation.
- **Findings**: `DatePicker.setMin`, `IntegerField.setMin/setStepButtonsVisible`, `EmailField`, `Binder` required and
  length validators, `Grid` with component column, `ConfirmDialog` (setCancelable), `QueryParameters` with
  `BeforeEnterObserver`, `FormLayout` responsive steps — all standard; no new dependency.
- **Implications**: No add-ons, no custom CSS beyond Lumo utility classes.

## Architecture Pattern Evaluation

| Option | Description | Strengths | Risks / Limitations | Notes |
|--------|-------------|-----------|---------------------|-------|
| Views call `ReservationService` only | Online rules in views | No new class | Business rules in views (violates `structure.md`) | Rejected |
| Small `GuestBookingService` + views | Service holds proof check, deadline, e-mail rule | Rules testable without UI, views thin | One more class | Selected |
| Separate confirmation route with code in the address | `booking-confirmation/{code}` | Bookmarkable | Anyone with the code sees guest data; breaks D1 | Rejected; confirmation is a state of the booking view |

## Design Decisions

### Decision: Identification by reservation code + e-mail address
- **Context**: domain.md open question; brief constraint "no guest account".
- **Alternatives Considered**: 1. Code only — too weak, codes are quoted on the phone. 2. Code + last name — names are
  short and guessable. 3. Code + e-mail — two secrets, e-mail is already collected. 4. Magic link by e-mail — needs
  e-mail delivery (out of scope).
- **Selected Approach**: 3; identical "not found" message for any mismatch so codes cannot be probed separately.
- **Trade-offs**: Phone-only reservations from the front desk cannot be opened online (acceptable, guest calls).
- **Follow-up**: No attempt limit (code space 31^8 plus e-mail); revisit if abuse is observed.

### Decision: Online cancel until the day before arrival; no online change
- **Context**: domain.md open question; core allows cancel any time while Reserved (D9).
- **Selected Approach**: Cancel allowed while Reserved and `today < arrivalDate`; change only by cancel and re-book or
  via the hotel.
- **Rationale**: Keeps the online flow to one destructive action; on the arrival day reception handles no-shows and
  late changes.

### Decision: Confirmation inside the booking view; no e-mail
- **Selected Approach**: After a successful booking the booking view replaces its form with the confirmation. A reload
  re-evaluates the selection, finds the room taken and shows no guest data (4.4).

### Synthesis outcomes
- **Generalization**: Confirmation and "My reservation" show the same reservation fields → one read-only
  `ReservationDetailsPanel`.
- **Build vs. adopt**: All UI from standard Vaadin components; all rules from reservation-core; no library added.
- **Simplification**: No main migration (no new tables); no public layout class (three views, each with its own
  heading and back links); availability is called directly from the view (no pass-through service method); no
  prefilled search when returning from the booking page.

## Risks & Mitigations
- Upstream contract drift (record components, messages) — design lists every used element; reservation-core
  revalidation triggers cover them.
- Look-up brute force — two secrets, non-sequential codes, identical messages; no rate limit (documented).
- Test isolation with parallel specs — own ids 4000–4999 and April 2027 dates; rows created by tests use
  2027-04-15–30 and are deleted after each test.
- Management tests changing nightly rates during a run could change expected prices — they restore rates; prices are
  asserted from the Double/Family seed rates only.

## References
- `.kiro/specs/reservation-core/design.md` — public contract, messages, test data conventions.
- `.kiro/specs/foundation/design.md` — `PublicHomeView`, security defaults, fixed clock.
- `.kiro/steering/vaadin-jooq.md`, `testing.md`, `domain.md` — conventions and vocabulary.
