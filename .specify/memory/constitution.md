<!--
Sync Impact Report
- Version change: 1.0.0 → 1.1.0 (MINOR: materially expanded guidance in Principle II)
- Modified principles: II. Fixed Stack (title unchanged) — allows spring-boot-starter-mail for guest
  emails, plain text through JavaMailSender, no other mail or templating library
- Added sections: none
- Removed sections: none
- Templates: plan/spec/tasks templates read this file at runtime; no template changes needed
- Deferred TODOs: none
-->

# Hotel Reservation System Constitution

## Core Principles

### I. Vision Is the Source

`docs/vision.md` defines the scope, roles and goals of the Hotel Reservation System (HRS) for one hotel
with ten rooms.

- Specs MUST use the vision's terms: roles Guest, Receptionist, Manager, Housekeeping; cleaning status
  dirty and clean. One name per concept.
- Features listed under "Out of scope" in the vision MUST NOT be specified or built.
- Staff (Receptionist, Manager, Housekeeping) log in with role-based access; the vision lists no login
  for the Guest.

### II. Fixed Stack

The stack is the one in `pom.xml`; no other framework or UI technology is added without amending this
constitution.

- UI: Vaadin Flow server-side Java views (no Hilla, no React/TypeScript views).
- Data access: jOOQ. No JPA/Hibernate.
- Spring Security is not in `pom.xml` yet; the first feature that needs staff login adds it.
- `spring-boot-starter-mail` is allowed for emails to guests (plain text through `JavaMailSender`); no
  other mail or templating library.
- Base package `ch.martinelli.edu.hrs`; the application class is `HrsApplication`.

### III. Schema Through Flyway, Data Through jOOQ

- Every schema change is a new Flyway migration in `src/main/resources/db/migration`. A committed
  migration MUST NOT be edited; add a new one.
- `./mvnw compile` runs Flyway against an H2 file database and generates jOOQ classes into
  `ch.martinelli.edu.hrs.db`. Generated code MUST NOT be edited.

Rationale: the generated jOOQ classes and the running database must always match the migrations.

### IV. Every View Is Tested

- Every view has Browserless tests that extend `core/ui/HrsBrowserlessTest` (run by `./mvnw test`).
- Every feature has at least one Playwright test that extends `core/ui/PlaywrightIT`, named `*IT`
  (run by `./mvnw verify`).
- Each scenario of a spec, including its alternative and error flows, is covered by a test.

### V. Green Build

`./mvnw verify` is the single quality gate. A feature or user story is done only when it ends green.

## Technology Constraints

Versions as in `pom.xml`: Java 25, Spring Boot 4.1.1, Vaadin 25.3.1, jOOQ and Flyway as managed by
Spring Boot, H2 (runtime), Playwright 1.63.0 with Mopo 0.0.6 (tests), Vaadin Browserless test (tests).

From the vision:

- Guest personal data is handled in line with GDPR.
- The staff UI is usable on a desktop at the reception desk and on a tablet for housekeeping.
- Single property with ten rooms; no scaling beyond that.

## Development Workflow

Features follow the Spec Kit flow described in `docs/spec-kit-workflow.md`: specify → clarify → plan →
tasks → analyze → implement → converge. Each `plan.md` passes a Constitution Check against this file
before tasks are generated. The last task of every feature runs `./mvnw verify`.

## Governance

This constitution overrides other practices in this repository. Amendments are made with
`/speckit-constitution`, reviewed by the project owner, and committed on their own.

Versioning follows semantic versioning: MAJOR for removed or redefined principles, MINOR for new
principles or sections, PATCH for wording fixes.

**Version**: 1.1.0 | **Ratified**: 2026-11-08 | **Last Amended**: 2026-11-08
