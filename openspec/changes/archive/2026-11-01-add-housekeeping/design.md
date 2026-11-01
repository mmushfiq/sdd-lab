# Design

## Context

Builds on `add-room-management` (table `room`, `StaffLayout`) and `add-staff-login` (Housekeeping role). Migrations
`V1`–`V3` exist. Requirements are in `specs/housekeeping/spec.md`.

## Goals / Non-Goals

**Goals:**

- One cleaning status per room that `add-check-in-out` reads and writes.

**Non-Goals:**

- Assigning rooms to individual housekeepers, cleaning history, "in progress" status.
- Live push of new dirty rooms to an open list (see Risks).
- Making a room dirty by hand; only check-out does that (`add-check-in-out`).

## Decisions

### 1. Column on `room` (migration `V4__add_room_cleaning_status.sql`)

| Column            | Type          | Constraints                                                          |
|-------------------|---------------|----------------------------------------------------------------------|
| `cleaning_status` | `VARCHAR(10)` | not null, default `'CLEAN'`, check `cleaning_status IN ('CLEAN','DIRTY')` |

Existing rooms become clean through the default. Alternative: a separate cleaning table with history. Rejected: no
requirement needs history.

### 2. Service

`HousekeepingService` in `ch.martinelli.edu.hrs.housekeeping`: `findDirtyRooms()` (join `room_type`, order by
`room_number`) and `markCleaned(roomId)`, an unconditional `UPDATE room SET cleaning_status = 'CLEAN'`, so marking
an already clean room is harmless.

### 3. View

| Class              | Package                                    | Route      | Access |
|--------------------|--------------------------------------------|------------|--------|
| `CleaningListView` | `ch.martinelli.edu.hrs.housekeeping.ui`    | `cleaning` | `@RolesAllowed("HOUSEKEEPING")`, `@AccessDeniedErrorRouter` |

- One card per dirty room (room number large, room type) with a "Cleaned" button (`ButtonVariant.LUMO_LARGE`, min.
  44 px high), stacked vertically, so it fits a 768 px tablet. A "Refresh" button reloads the list.
- After "Cleaned", the list reloads from the database.

### 4. Test data

Until `add-check-in-out`, nothing makes a room dirty. Tests set `cleaning_status = 'DIRTY'` with jOOQ and reset it
afterwards.

## Risks / Trade-offs

- [The goal "after check-out, housekeeping sees the room immediately" needs the list to update] → The list reloads
  when opened, on "Refresh" and after each "Cleaned". Server push can be added later if housekeeping keeps the page
  open for long.

## Migration Plan

New migration `V4`; existing rooms get `CLEAN` by default.
