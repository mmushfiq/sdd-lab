# Vaadin, jOOQ and Flyway Conventions

How views, data access and schema changes are written in HRS. The layering and package rules are in
`structure.md`; this file covers how to use the three frameworks inside them.

## Philosophy
- The schema is the source of truth: Flyway defines it, jOOQ generates the Java model from it.
- Keep SQL in repositories, rules in services, and views as thin Java UI.
- Prefer built-in Vaadin components and jOOQ DSL features over hand-made helpers.

## Flyway Migrations

- Location: `src/main/resources/db/migration`, named `V<n>__<snake_case_description>.sql`, `<n>` ascending
  without gaps.
- A committed migration is never edited. Changes go into a new migration.
- One migration per coherent change (a feature's tables, a new column), not one per table.
- Reference data that every environment needs (for example the ten rooms, room types) goes into main migrations;
  test-only data goes into `src/test/resources/db/migration`.
- SQL must run on H2 (the build and runtime database). Avoid vendor features H2 lacks.
- The build migrates a file database under `target/`. If a local migration was changed before it was committed,
  `./mvnw clean compile` rebuilds it (otherwise Flyway reports a checksum mismatch).

## Schema Design

- Tables and columns: `snake_case`, table names singular (`room`, `room_type`, `reservation`).
- Primary key `id BIGINT` with a sequence or identity; foreign keys `<table>_id` with a declared `FOREIGN KEY`.
- Every column is `NOT NULL` unless the domain says the value can be missing.
- Money: `DECIMAL(10,2)` → `BigDecimal`. Dates of stay: `DATE` → `LocalDate`. Points in time: `TIMESTAMP WITH TIME
  ZONE` → `OffsetDateTime`.
- Statuses: `VARCHAR` plus a `CHECK` constraint listing the allowed values, mapped to a Java enum in code.
- Let the database enforce what it can: `UNIQUE` (room number, user name), `CHECK` (departure after arrival,
  capacity > 0), foreign keys. Rules spanning rows (no overlapping reservations) are enforced in the service inside
  a transaction.
- Booking nights are half-open ranges: arrival inclusive, departure exclusive. Two stays overlap when
  `a.arrival < b.departure AND b.arrival < a.departure`.

## jOOQ Repositories

- A repository is a Spring `@Repository` with a constructor-injected `DSLContext`; it holds SQL only, no rules.
- Refer to tables through the generated classes (`static import ch.martinelli.edu.hrs.db.Tables.*`); never write
  table or column names as strings.
- Map results to Java records with `fetchInto(...)` or `Records.mapping(...)`; do not expose generated `*Record`
  classes to views.
- Select only the columns a use case needs; join in SQL instead of looping with a query per row.
- Return `Optional` for single lookups (`fetchOptional`), `List` for collections.

```java
@Repository
class RoomRepository {
    private final DSLContext dsl;

    RoomRepository(DSLContext dsl) { this.dsl = dsl; }

    List<RoomSummary> findAvailable(LocalDate arrival, LocalDate departure, int guests) {
        return dsl.select(ROOM.ID, ROOM.NUMBER, ROOM_TYPE.NAME, ROOM_TYPE.NIGHTLY_RATE)
                  .from(ROOM).join(ROOM_TYPE).on(ROOM.ROOM_TYPE_ID.eq(ROOM_TYPE.ID))
                  .where(ROOM_TYPE.CAPACITY.ge(guests))
                  .andNotExists(selectOne().from(RESERVATION)
                          .where(RESERVATION.ROOM_ID.eq(ROOM.ID))
                          .and(RESERVATION.ARRIVAL.lt(departure))
                          .and(RESERVATION.DEPARTURE.gt(arrival)))
                  .fetchInto(RoomSummary.class);
    }
}
```
_The table and column names are examples; the real ones come from the specs' designs._

## Services and Transactions

- A service is a Spring `@Service`; public methods that change data are `@Transactional`.
- Check-then-write rules (availability before insert, clean status before check-in) run in one transaction, with
  a row lock where two users can race (`SELECT ... FOR UPDATE` via `forUpdate()` on the room).
- A broken business rule is reported with a dedicated exception carrying a user-readable message; the view shows
  it, it does not re-check the rule itself.
- Services take and return records or simple values, never Vaadin types.
- Read "today" and "now" from an injected `java.time.Clock`, never from `LocalDate.now()` without a clock, so tests
  can fix the date.

## Vaadin Flow Views

- A view is a Java class with `@Route("kebab-case")` and `@PageTitle`, extending a layout component
  (`VerticalLayout`, `Div`, ...). Staff views share one main layout via `@Route(layout = ...)`.
- Views receive services by constructor injection and never touch `DSLContext` or repositories.
- Use standard components: `Grid` for lists, `FormLayout` + `Binder` for forms, `DatePicker` for dates,
  `ComboBox`/`Select` for choices, `ConfirmDialog` before destructive actions, `Notification` for results.
- Validation: field-level rules (required, ranges, formats) in the `Binder`; business rules come back from the
  service as exceptions and are shown in the form or a notification.
- Route parameters through `HasUrlParameter` or `BeforeEnterObserver`; a view must fully refresh its content when
  the parameter changes, because Vaadin reuses the instance when navigating to the same route.
- Give components that tests or users need to find a kebab-case id (`grid.setId("rooms")`).
- Layouts must work on a desktop (reception, manager) and a tablet (housekeeping): prefer responsive `FormLayout`
  steps and Lumo utility classes over fixed widths.
- Styling: Lumo theme and utility classes first; project CSS only in `META-INF/resources/styles.css`.

## Access Control in Views

- Guest-facing views are public; staff views are restricted by role on the view class (Spring Security with
  Vaadin's view access checking: `@AnonymousAllowed`, `@RolesAllowed`).
- Navigation menus show only the entries the current role may open.
- Hiding a button is not access control: the view (and, for sensitive operations, the service) must reject the
  action too.

## Anti-Patterns
- SQL or jOOQ in a view; business rules in a view or a repository.
- Editing a committed migration or the generated `ch.martinelli.edu.hrs.db` classes.
- Using `LocalDate.now()` / `Instant.now()` directly in rules.
- One query per grid row to load related data.

---
_Focus on patterns and decisions. Concrete tables and screens belong in the specs._
