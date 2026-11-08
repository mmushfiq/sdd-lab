# Research: Hotel Reservation System

Decisions for the open technical points of [plan.md](plan.md). Stack and versions are fixed by the
constitution; nothing here adds a framework.

## R1 Staff login and roles

- **Decision**: Add `spring-boot-starter-security` (and `spring-boot-starter-security-test`, test scope). A
  `SecurityConfiguration` uses `VaadinSecurityConfigurer.vaadin()` with `loginView(LoginView.class)`. Each
  view carries `@RolesAllowed("RECEPTIONIST" | "MANAGER" | "HOUSEKEEPING")` or `@AnonymousAllowed` (guest
  views, login). Staff users are read from the `staff_user` table by a jOOQ-based `UserDetailsService`;
  deactivated users are returned as disabled. Passwords use Spring's delegating encoder (bcrypt).
- **Rationale**: The constitution says the first feature that needs staff login adds Spring Security.
  `VaadinSecurityConfigurer` is the Vaadin 25 way (replaces the removed `VaadinWebSecurity`) and enables
  view-based access control with no extra code.
- **Alternatives**: in-memory users (rejected: FR-025 needs the Manager to manage users); plain
  `formLogin()` (rejected: Vaadin docs warn it causes redirect loops).

## R2 Initial Manager account

- **Decision**: A Flyway migration inserts one Manager `manager` with a bcrypt hash of a documented initial
  password (see [quickstart.md](quickstart.md)). The Manager resets it on first use through staff user
  management (FR-025).
- **Rationale**: Spec assumption "one initial Manager account is provided at installation"; a migration is
  the existing way to put data into the database, no startup code needed.
- **Alternatives**: create the Manager from application properties at startup (more code, same result for
  a single hotel).
- **Risk**: The initial password is known until changed; quickstart says to reset it first.

## R3 No double bookings (FR-007, SC-001)

- **Decision**: Booking and changing a reservation run in one transaction that first locks the room row
  (`SELECT … FOR UPDATE` on `room`), then checks for an overlapping booked/checked-in reservation
  (`arrival < newDeparture AND departure > newArrival`), then inserts/updates. A concurrency test books the
  same room from two threads and expects exactly one success.
- **Rationale**: Serializes bookings per room; with ten rooms there is no contention problem. H2 supports
  row locks with `FOR UPDATE`.
- **Alternatives**: one row per booked night with a unique (room, night) index (DB-level guarantee, but
  extra table kept in sync with status changes); H2 has no exclusion constraints.

## R4 Reservation number

- **Decision**: 8 characters from `ABCDEFGHJKLMNPQRSTUVWXYZ23456789` drawn with `SecureRandom`, unique
  constraint in the database, regenerate on collision.
- **Rationale**: Not guessable, so online lookup (number + email) cannot be enumerated; easy to read out on
  the phone.
- **Alternatives**: sequential number (guessable, reveals booking volume).

## R5 "Today" and dates

- **Decision**: A `Clock` bean (system default zone) is injected wherever "today" matters (search
  validation, check-in, guest cancellation, anonymization, room overview). Tests use dates relative to
  `LocalDate.now(clock)`.
- **Rationale**: Keeps date rules testable without changing system time.
- **Alternatives**: calling `LocalDate.now()` directly (tests around "today" become flaky near midnight and
  cannot fix a date); a date passed in from the UI (spreads the rule across views).

## R6 Guest data and anonymization (FR-024, FR-026)

- **Decision**: Guest name, email and phone are columns of `reservation` (no separate guest table). A
  `@Scheduled` daily job (03:00) anonymizes reservations whose departure date (or cancellation) is more than
  12 months ago: name → `Anonymized`, email and phone → `NULL`, `anonymized = true`. The logic is a service
  method so tests call it directly.
- **Rationale**: Every reservation has exactly one guest and the spec makes the Guest no account; a separate
  table would add joins for no gain.
- **Alternatives**: separate `guest` table deduplicated by email (needs merge rules the spec does not ask
  for).

## R7 Cleaning list freshness (FR-020, SC-005)

- **Decision**: The cleaning list reads current data when opened and polls every 30 seconds
  (`UI.setPollInterval`), plus a refresh button.
- **Rationale**: Meets "no manual step by reception" without server push configuration.
- **Alternatives**: `@Push` with broadcasts on check-out (more moving parts for one screen).

## R8 Deleting rooms and room types

- **Decision**: Foreign keys block deleting a room type used by a room and a room that has any reservation
  (also past ones); the UI shows the reason. Rooms with history are kept so reports stay correct.
- **Rationale**: FR-003 asks to reject the booked/checked-in case; past reservations also need their room
  for reports, so the stricter rule is simpler and safe.
- **Alternatives**: block only rooms with booked/checked-in reservations and delete or detach the past ones
  (loses report history); a soft "inactive" flag on rooms (not asked for by the spec).

## R9 Navigation by role

- **Decision**: One `MainLayout` (app layout + side navigation) built from Vaadin's menu entries
  (`@Menu` on views, `MenuConfiguration.getMenuEntries()`), which lists only views the current user may
  open. After login a user lands on their role's first view; guests use public views without the staff
  menu.
- **Rationale**: Built into Vaadin 25, no custom role-to-menu mapping.
- **Alternatives**: a hand-written menu with role checks per item (duplicates the access rules of the
  views); a separate layout per role (three layouts to keep in sync).

## R10 Testing approach

- **Decision**:
  - Service tests (`@SpringBootTest`, `@Transactional` rollback) for availability, double booking
    (including the two-thread test, not transactional), state transitions, reports and anonymization.
  - Browserless tests per view extending `HrsBrowserlessTest`, with `@WithMockUser(roles = …)` for staff
    views.
  - One Playwright test (`*IT`, extends `PlaywrightIT`) per user story covering its main scenario; staff
    tests log in through the login view.
  - Tests create their own rooms and reservations and delete what they created; no shared test-data
    migration.
- **Rationale**: Constitution principle IV; keeps tests independent of each other.
- **Alternatives**: a shared test-data migration (tests depend on each other's data and on order); only
  Playwright tests (slow, and rule edge cases are hard to reach through the browser).

## R11 UI validation and language

- **Decision**: Vaadin `Binder` validators for forms (required fields, email format, date rules); service
  methods re-check the same rules and throw an exception with a user-facing message. All texts in English
  (FR-027), no i18n bundle.
- **Rationale**: No bean-validation dependency is in `pom.xml`; English only needs no translation setup.
- **Alternatives**: Jakarta Bean Validation with `BeanValidationBinder` (adds a dependency the constitution
  does not list); Vaadin i18n translation files (not needed for one language).
