# Technology Stack

## Architecture

A single Spring Boot monolith with a server-side Java UI. Vaadin Flow views call Spring services; services own the
business rules and transactions; data access is plain jOOQ against a schema owned by Flyway. There is no REST API and
no separate frontend codebase: everything, including the UI, is written in Java.

```
Vaadin view (UI) → Spring service (rules, @Transactional) → jOOQ repository (SQL) → H2
```

## Core Technologies

- **Language**: Java 25
- **Framework**: Spring Boot 4.1
- **UI**: Vaadin 25 Flow (Java views, Lumo theme); no Hilla/React views
- **Data access**: jOOQ with code generated from the database schema
- **Schema**: Flyway migrations
- **Database**: H2 (embedded, no external setup)
- **Build**: Maven via the wrapper `./mvnw`

## Key Libraries

- **jOOQ code generation**: the generated classes live in `ch.martinelli.edu.hrs.db` and are the only way to refer to
  tables and columns. They are build output, never edited.
- **Vaadin Browserless** (`browserless-test-junit6`, `browserless-test-spring`): server-side UI tests without a browser.
- **Playwright + Mopo**: browser tests against the running app; Mopo helps with Vaadin components.
- **Spring Security**: not yet a dependency. Login and role-based access are in scope, so the feature that introduces
  them adds it.

## Development Standards

### Type Safety
- Use the generated jOOQ table and record classes; no SQL strings and no hand-written column names.
- Prefer Java records for data passed between layers.

### Schema Changes
- Every schema change is a new versioned migration in `src/main/resources/db/migration` (`V<n>__<description>.sql`).
- Never edit a migration that has been committed; add a new one.
- After a schema change, `./mvnw compile` regenerates the jOOQ classes.

### Code Quality
- Business rules live in services, not in views or SQL-only constraints; important invariants (no overlapping
  reservations, clean-before-check-in) are also protected by the database where practical.
- Use Vaadin's built-in components and Lumo utility classes before writing custom CSS (`styles.css`).

### Testing
- **Unit / UI tests** (`./mvnw test`, Surefire): Browserless tests extend `core.ui.HrsBrowserlessTest`
  (`@SpringBootTest`, English locale).
- **Integration tests** (`./mvnw verify`, Failsafe): classes ending in `IT`, extending `core.ui.PlaywrightIT`
  (random port, headless Chromium).
- Always run `verify`, not `integration-test`, so Failsafe failures fail the build.

## Development Environment

### Required Tools
- Java 25, Maven (or `./mvnw`); Node is managed by the Vaadin build.
- Optional: GitHub Codespaces (`.devcontainer`) with Java, Maven, Node and Playwright's Chromium.

### Common Commands
```bash
# Dev:    ./mvnw spring-boot:run          (http://localhost:8080)
# Build:  ./mvnw compile                  (Flyway migrate + jOOQ codegen)
# Test:   ./mvnw test                     (Browserless)
# All:    ./mvnw verify                   (Browserless + Playwright *IT)
```

## Key Technical Decisions

- **Schema first, generated access**: the build runs Flyway against a file-based H2 database in `target/` during
  `generate-sources`, then jOOQ generates code from it. The schema in the migrations is the source of truth; the code
  follows it.
- **Server-side UI only**: Vaadin Flow keeps UI, rules and data access in one Java codebase and one test setup.
- **Embedded H2**: fits a ten-room, single-property system; no database server to run.
- **Two test levels**: Browserless for fast, wide coverage of views and rules; Playwright for a few end-to-end checks
  in a real browser.

---
_Document standards and patterns, not every dependency_
