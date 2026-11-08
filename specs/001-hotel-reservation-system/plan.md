# Implementation Plan: Hotel Reservation System

**Branch**: `001-hotel-reservation-system` | **Date**: 2026-11-08 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/001-hotel-reservation-system/spec.md`

## Summary

A single-hotel reservation system with public guest booking and logged-in staff screens for reception,
housekeeping and the manager. It is built on the existing Spring Boot / Vaadin Flow / jOOQ / Flyway / H2
project: server-side Java views per role, services that use jOOQ's `DSLContext` directly, Flyway migrations
for four tables (`room_type`, `room`, `reservation`, `staff_user`), and Spring Security with Vaadin's
view-based access control for the three staff roles. Double bookings are prevented by locking the room row
before the overlap check ([research.md](research.md) R3).

## Technical Context

**Language/Version**: Java 25

**Primary Dependencies**: Spring Boot 4.1.1, Vaadin 25.3.1 (Flow), jOOQ and Flyway (versions managed by Spring
Boot); new: `spring-boot-starter-security` (R1)

**Storage**: H2 file database; schema via Flyway, jOOQ classes generated into `ch.martinelli.edu.hrs.db`

**Testing**: JUnit (via Spring Boot), Vaadin Browserless test (`HrsBrowserlessTest`), Playwright 1.63.0 with Mopo
(`PlaywrightIT`); new test dependency `spring-boot-starter-security-test` for `@WithMockUser`

**Target Platform**: Server-side web app on the JVM; browsers on the reception desktop and housekeeping tablet

**Project Type**: Web application (single Maven module, server-side UI)

**Performance Goals**: Online booking under 3 minutes for a guest (SC-003); check-in/out under 1 minute
(SC-004) — UX targets, no throughput target

**Constraints**: English UI only; GDPR anonymization after 12 months; staff screens usable on desktop, cleaning
list on tablet

**Scale/Scope**: 1 hotel, 10 rooms, a handful of staff users, 10 views, 4 tables

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Check | Result |
|---|---|---|
| I. Vision is the source | Roles and terms (Guest, Receptionist, Manager, Housekeeping, dirty/clean) used as in the vision; no out-of-scope feature (payment, channels, extras, multi-hotel, loyalty) planned | Pass |
| II. Fixed stack | Vaadin Flow views only, jOOQ only, no JPA; Spring Security added because this feature needs staff login (allowed by II); its test starter belongs to it | Pass |
| III. Flyway / jOOQ | Schema only through new migrations; generated code untouched | Pass |
| IV. Every view tested | Browserless test per view, one Playwright `*IT` per user story, service tests for rules (R10) | Pass |
| V. Green build | Last task of every user story runs `./mvnw verify` | Pass |

Post-design re-check (after data-model, contracts, quickstart): no new framework, layer or module was
introduced; still **Pass**. Complexity Tracking is empty.

## Project Structure

### Documentation (this feature)

```text
specs/001-hotel-reservation-system/
├── plan.md              # This file
├── research.md          # Phase 0: decisions R1–R11
├── data-model.md        # Phase 1: tables, rules, status transitions
├── quickstart.md        # Phase 1: run and manual walk-through
├── contracts/
│   └── ui-routes.md     # Phase 1: routes, views, roles
└── tasks.md             # Phase 2 (/speckit-tasks, not created here)
```

### Source Code (repository root)

```text
src/main/java/ch/martinelli/edu/hrs/
├── HrsApplication.java                 # existing; add @EnableScheduling (R6)
├── core/
│   ├── security/   SecurityConfiguration, LoginView, StaffUserDetailsService
│   ├── ui/         MainLayout (side menu from @Menu entries, logout)
│   └── ClockConfiguration              # Clock bean (R5)
├── room/           RoomTypeService, RoomService, RoomTypeView, RoomView            # US1
├── staff/          StaffUserService, StaffUserView                                 # US1
├── reservation/    AvailabilityService, ReservationService, ReservationNumberGenerator,
│                   ReservationView (reception), AnonymizationJob                    # US2, US6, FR-026
├── frontdesk/      FrontDeskView (arrivals, departures, check-in/out, room overview) # US4
├── housekeeping/   CleaningListView                                                # US5
├── guest/          GuestBookingView, MyReservationView                             # US3, US7
└── report/         ReportService, ReportView                                       # US8

src/main/resources/db/migration/
├── V1__room_type_and_room.sql
├── V2__staff_user.sql                  # incl. initial Manager (R2)
└── V3__reservation.sql

src/test/java/ch/martinelli/edu/hrs/
├── core/ui/        HrsBrowserlessTest, PlaywrightIT (existing base classes)
└── <same packages as main>/  *ServiceTest, *ViewTest (Browserless), *IT (Playwright)
```

**Structure Decision**: One Maven module, packages by feature area under the existing base package
`ch.martinelli.edu.hrs`. Views call services; services use the generated jOOQ classes through `DSLContext`; no
repository or DTO layer. Status values are Java enums mapped to the `VARCHAR` columns.

## Complexity Tracking

No constitution violations.
