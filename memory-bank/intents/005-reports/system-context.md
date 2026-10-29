---
intent: 005-reports
phase: inception
status: context-defined
updated: 2026-10-29T07:58:01Z
---

# Reports - System Context

## System Overview

A Manager-only report view in the HRS application, calculated from reservations and rooms.

## Actors

- **Manager** (Human, desktop): selects a month and reads occupancy and revenue.

## Context Diagram

```mermaid
C4Context
    title System Context - 005-reports

    Person(manager, "Manager", "Runs the business")
    System(hrs, "Hotel Reservation System", "Monthly occupancy and revenue report")

    Rel(manager, hrs, "Views monthly report", "Browser")
```

## External Integrations

- None.

## Data Flows

### Inbound
- Selected month/year.

### Outbound
- Totals and per-room-type rows on screen.

## High-Level Constraints

- Manager role only (intent 004).

## Key NFR Goals

- Exact figures (verified against a hand-calculated month); report in under 2 s.
