# Research & Design Decisions

## Summary
- **Feature**: `front-desk-reservations`
- **Discovery Scope**: Extension (light discovery). Four staff views on top of contracts that foundation and
  reservation-core already define; no new table, no new service, no new dependency.
- **Key Findings**:
  - `ReservationService` (reservation-core) offers everything the pages need: `findAvailableRooms`, `create`,
    `change`, `cancel`, `findById`, `findByCode`, `search`. No required upstream change.
  - Availability does not disregard the reservation being changed, so on the change page the current room would be
    missing from `findAvailableRooms` whenever the new range overlaps its own nights. Solved in the view by always
    offering the current room; `change()` itself disregards the reservation (reservation-core 6.1) and decides.
  - Foundation's extension contract (`@Route(layout = StaffLayout.class)`, `@Menu`, `@RolesAllowed`) adds the pages
    and menu entries without touching foundation code; `StaffLayout` falls back to the principal name so
    `@WithMockUser` tests work.

## Research Log

### Upstream contracts
- **Context**: The brief requires using reservation-core and foundation exactly as defined.
- **Sources Consulted**: `.kiro/specs/reservation-core/design.md` (Service Interface, Error Handling, test data),
  `.kiro/specs/foundation/design.md` (StaffLayout extension contract, StaffRole.Names, test accounts, fixed clock).
- **Findings**:
  - `ReservationDetails` has `id`, `code`, `status`, `roomId`, `roomNumber`, `roomTypeName`, dates, guests,
    `nightlyRate`, `guest` (null when erased) and derived `nights()`, `totalPrice()` — all the reservation page needs.
  - `AvailableRoom` carries capacity, current rate and range price — enough for the new-reservation room grid.
  - `ReservationException.reason()` distinguishes `NOT_FOUND`, `INVALID_INPUT`, `ROOM_NOT_AVAILABLE`,
    `TRANSITION_NOT_ALLOWED`; messages are user-readable and free of guest data.
  - `search(new ReservationSearch(name, from, to))` is open-ended when one date is null and refuses `to <= from`.
  - `findByCode` trims and ignores case.
  - Service methods carry no role checks; access is the calling view's job (reservation-core D9, Security).
- **Implications**: Views catch only `ReservationException`, show `getMessage()` unchanged (10.1), and need no own
  rule code. The reservation id is the stable key for the page address.

### Room choice on the change page
- **Context**: Requirement 7.2 — the receptionist picks a room for the changed stay.
- **Findings**: `findAvailableRooms(arrival, departure, guests)` counts the reservation itself as occupying, so an
  extension in the same room would hide the current room. No room listing service exists outside management.
- **Implications**: Options = current room (from `ReservationDetails`) + available rooms. No price preview on the
  change page (the booked-rate rule is reservation-core's, D2); the reservation page shows the result.
- **Upstream note (optional, not required)**: an overload `findAvailableRooms(..., excludedReservationId)` in
  reservation-core would allow a price preview; not requested because the requirements do not need it.

### Vaadin specifics
- **Route templates**: `reservations/:reservationId([0-9]{1,18})` and `.../change` use a digits-only regex, so the
  static route `reservations/new` can never match the parameter route; no precedence question arises.
- **Instance reuse**: Vaadin reuses a view instance when navigating to the same route with another parameter; every
  view loads in `beforeEnter` (steering `vaadin-jooq.md`).
- **Enter shortcuts**: `Button.addClickShortcut(Key.ENTER).listenOn(<field container>)` scopes Enter to one group of
  fields, so the code field and the search fields on the list page do not trigger each other.
- **Date format**: `DatePicker.DatePickerI18n#setDateFormat("yyyy-MM-dd")` gives ISO typing and display (F5).
- **ConfirmDialog**: part of the Vaadin 25 core components in `vaadin-spring-boot-starter` (steering lists it as
  the standard for destructive actions).
- **Testing**: keyboard behaviour (focus, Enter, Tab) is verified in the Playwright test with real key presses;
  Browserless tests click buttons, because scoped shortcuts are a client-side mechanism.

## Architecture Pattern Evaluation

| Option | Description | Strengths | Risks / Limitations | Notes |
|--------|-------------|-----------|---------------------|-------|
| Views only (selected) | Four `@Route` views calling `ReservationService` directly | Thin, no duplicated rules, matches steering | Views hold some UI state (search results, options) | No own service: there is no rule to own |
| Own facade service | `ReservationDeskService` wrapping `ReservationService` | Place for future rules | Pure pass-through, speculative | Rejected (simplification) |
| One page with dialogs | List with create/change/cancel dialogs | Fewer routes | No linkable reservation page for reception | Rejected: 4.6 needs an address |

## Design Decisions

### Decision: Receptionist only (F1)
- **Context**: Brief asks whether the manager may also manage reservations.
- **Alternatives**: 1. Receptionist only. 2. Receptionist and manager.
- **Selected**: Receptionist only, via `@RolesAllowed(StaffRole.Names.RECEPTIONIST)` on every view.
- **Rationale**: `product.md`/`domain.md` give reservations to the receptionist; brief says "receptionist-only".
  Adding the manager later is a one-line annotation change per view (revalidation trigger).

### Decision: Package `ch.martinelli.edu.hrs.reservation.ui`
- **Context**: `structure.md` puts views in a `<feature>/ui` subpackage; reservation-core owns `reservation`;
  `domain.md` lists "front desk" as an avoid word, so a `frontdesk` package is not allowed.
- **Selected**: views and `DeskFormats` live in `reservation.ui`; no service class is added because no rule belongs here.
- **Rationale**: follows the `<feature>/ui` pattern; `reservation.ui` is a different Java package from `reservation`,
  so reservation-core's package-private `ReservationRepository` stays unreachable and only its public types are used.
  Class names (`ReservationListView`, ...) do not collide with reservation-core's classes; guest-booking uses
  `guestbooking`.

### Decision: No price preview when changing (F6)
- **Context**: Only reservation-core knows whether the booked rate is kept (D2).
- **Selected**: change page shows room choices by number and type only; the reservation page shows the resulting rate
  and price after saving.
- **Rationale**: computing the price in the view would duplicate the pricing rule.

### Decision: Errors shown inline, results as notifications
- **Selected**: refusals in a `Span` `error-message` next to the form (stays visible while correcting); successes as
  `Notification` naming only the code (10.2).

### Synthesis outcomes
- **Generalization**: all four views show `ReservationException` messages the same way; one package-private helper
  `DeskFormats` provides the ISO date picker and the amount format used by three views (F5).
- **Build vs. adopt**: standard Vaadin components only (`Grid`, `DatePicker`, `IntegerField`, `Select`,
  `ConfirmDialog`, `RouterLink`, `Notification`); no add-on.
- **Simplification**: no facade service, no own migration (V40–V49 stay unused), no shared "stay form" component —
  create and change differ in room choice and price display, so sharing would need flags.

## Risks & Mitigations
- Stale page (reservation changed by someone else) — every action goes through the service, which re-checks under
  lock; the view shows the message and reloads (6.4, 7.5, 8.4).
- Unexpected infrastructure exceptions reach Vaadin's default error handler, which logs stack traces; reservation-core
  messages contain no guest data, and views never log or put guest data into exception messages (10.3).
- Parallel spec reception may want a check-in action on the reservation page — out of boundary; reception links to
  the page address and offers check-in on its own pages (revalidation trigger if that changes).

## References
- `.kiro/specs/reservation-core/design.md` — service contract, messages, test data windows.
- `.kiro/specs/foundation/design.md` — StaffLayout extension contract, roles, test accounts, fixed clock.
- `.kiro/steering/vaadin-jooq.md`, `testing.md`, `structure.md`, `domain.md`, `roadmap.md` (reserved numbers).
