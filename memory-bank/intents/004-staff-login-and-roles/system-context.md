---
intent: 004-staff-login-and-roles
phase: inception
status: context-defined
updated: 2026-10-29T07:58:01Z
---

# Staff Login and Roles - System Context

## System Overview

Spring Security protects all staff views of the HRS application; staff accounts live in the HRS database. Guest pages stay public.

## Actors

- **Manager** (Human): manages staff accounts; full access.
- **Receptionist** (Human): front-desk and reservation functions.
- **Housekeeping** (Human): cleaning list.
- **Guest** (Human): public `/booking` pages only, no login.

## Context Diagram

```mermaid
C4Context
    title System Context - 004-staff-login-and-roles

    Person(manager, "Manager", "Full access, manages staff users")
    Person(receptionist, "Receptionist", "Front desk")
    Person(housekeeping, "Housekeeping", "Cleaning list")
    Person_Ext(guest, "Guest", "Public pages, no login")
    System(hrs, "Hotel Reservation System", "Spring Security + Vaadin, users in H2")

    Rel(manager, hrs, "Logs in, manages users", "Browser")
    Rel(receptionist, hrs, "Logs in", "Browser")
    Rel(housekeeping, hrs, "Logs in", "Tablet")
    Rel(guest, hrs, "Uses /booking without login", "Browser")
```

## External Integrations

- None (no identity provider).

## Data Flows

### Inbound
- Credentials at login; user data and passwords from the Manager.

### Outbound
- Session cookie; role-filtered navigation.

## High-Level Constraints

- Initial manager password from configuration.

## Key NFR Goals

- Every staff view and service is role-protected; passwords BCrypt-hashed.
