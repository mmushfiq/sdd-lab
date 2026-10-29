---
intent: 003-check-in-check-out-housekeeping
phase: inception
status: context-defined
updated: 2026-10-29T07:58:01Z
---

# Check-in, Check-out and Housekeeping - System Context

## System Overview

Front-desk and housekeeping functions in the HRS application, built on rooms and reservations from intent 001.

## Actors

- **Receptionist** (Human, desktop): checks guests in and out, uses the room overview.
- **Housekeeping** (Human, tablet): uses the cleaning list, marks rooms cleaned.
- **Guest** (Indirect): present at the desk.

## Context Diagram

```mermaid
C4Context
    title System Context - 003-check-in-check-out-housekeeping

    Person(receptionist, "Receptionist", "Front desk")
    Person(housekeeping, "Housekeeping", "Cleans rooms, uses a tablet")
    System(hrs, "Hotel Reservation System", "Room overview, check-in/out, cleaning list")

    Rel(receptionist, hrs, "Checks in/out, views room overview", "Browser")
    Rel(housekeeping, hrs, "Views cleaning list, marks rooms clean", "Tablet browser")
```

## External Integrations

- None.

## Data Flows

### Inbound
- Check-in / check-out actions, "mark cleaned" actions.

### Outbound
- Room overview, cleaning list, amount due at check-out.

## High-Level Constraints

- Hotel time zone Europe/Zurich.

## Key NFR Goals

- Zero check-ins to dirty rooms; check-in/out under 1 minute; tablet-friendly cleaning list.
