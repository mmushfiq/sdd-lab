# Project Structure

## Organization Philosophy

Feature-first packages under the root package `ch.martinelli.edu.hrs`. Each feature package holds its services and
repositories; its Vaadin views sit in a `ui` subpackage. Shared, feature-independent code lives in `core`. Feature
packages follow the business areas (for example reservations, reception, housekeeping, management, reports), so a
spec usually maps to one package.

## Directory Patterns

### Feature package
**Location**: `src/main/java/ch/martinelli/edu/hrs/<feature>/`
**Purpose**: services (business rules, transactions) and jOOQ repositories for one business area.
**Example**: `reception/CheckInService.java`, `reception/RoomAssignmentRepository.java`

### Feature views
**Location**: `src/main/java/ch/martinelli/edu/hrs/<feature>/ui/`
**Purpose**: Vaadin Flow views (`@Route`) and UI-only helpers for that feature. Views call services, never
repositories or jOOQ directly.
**Example**: `reception/ui/CheckInView.java`

### Shared core
**Location**: `src/main/java/ch/martinelli/edu/hrs/core/` (UI-wide parts in `core/ui/`)
**Purpose**: cross-feature infrastructure such as the main layout, security setup and clock configuration.
**Example**: `core/ui/HrsBrowserlessTest.java` and `core/ui/PlaywrightIT.java` are the test base classes.

### Generated jOOQ code
**Location**: `ch.martinelli.edu.hrs.db` (generated into `target/generated-sources/jooq`)
**Purpose**: table, record and key classes from the schema. Never edited, never committed.

### Migrations
**Location**: `src/main/resources/db/migration/`
**Purpose**: versioned Flyway scripts that define the schema and reference data.
**Example**: `V1__create_room.sql`

### Tests
**Location**: `src/test/java/ch/martinelli/edu/hrs/<feature>/...`, mirroring the main package of the code under test.
**Purpose**: Browserless tests (`*Test`) and Playwright tests (`*IT`). Test-only data goes into test migrations under
`src/test/resources/db/migration/`, never into main migrations.

### Static resources
**Location**: `src/main/resources/META-INF/resources/` (`styles.css`, loaded by `HrsApplication`)

## Naming Conventions

- **Classes**: PascalCase with a role suffix: `*View`, `*Service`, `*Repository`, `*Layout`.
- **Tests**: `<Subject>Test` for Browserless/unit tests, `<Subject>IT` for Playwright.
- **Packages**: lower case, singular business term (`reception`, `housekeeping`), `ui` for views.
- **Routes**: kebab-case (`check-in`, `room-types`).
- **Component IDs**: kebab-case `setId(...)` on components that tests look up (grids, messages, key fields).
- **Migrations**: `V<n>__<snake_case_description>.sql`.

## Import Organization

```java
import ch.martinelli.edu.hrs.reception.CheckInService;   // own code: full package, no wildcards
import static ch.martinelli.edu.hrs.db.Tables.ROOM;      // jOOQ tables via static import
import com.vaadin.flow.component.grid.Grid;
```

Java has no path aliases; use fully qualified package imports and keep the default IDE ordering.

## Code Organization Principles

- **Dependencies point inward**: `ui` → service → repository → jOOQ. A feature may call another feature's service,
  never its repository or its views.
- **One owner per table**: each table is written by the repository of the feature that owns it; others read through
  that feature's service or a read-only query in their own repository.
- **Views stay thin**: layout, binding and navigation only; validation that is a business rule belongs in the service.
- **`core` knows no feature**: core code must not import feature packages.

---
_Document patterns, not file trees. New files following patterns shouldn't require updates_
