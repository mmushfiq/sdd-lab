# Design

## Context

Builds on `add-staff-login`: Spring Security with view access annotations, `AccessDeniedView`, `StaffStartView` at
route `staff`, migration `V1`. Requirements are in `specs/room-management/spec.md`.

## Goals / Non-Goals

**Goals:**

- Room data that later changes (reservations, housekeeping, reports) reference by foreign key.
- A staff layout that later staff views plug into.

**Non-Goals:**

- Deleting rooms or room types (see proposal).
- Room photos, descriptions, amenities.

## Decisions

### 1. Tables (migration `V2__create_room.sql`)

`room_type`:

| Column         | Type            | Constraints                         |
|----------------|-----------------|-------------------------------------|
| `id`           | `BIGINT`        | primary key, generated identity     |
| `name`         | `VARCHAR(50)`   | not null, unique                    |
| `capacity`     | `INT`           | not null, check `capacity >= 1`     |
| `nightly_rate` | `DECIMAL(10,2)` | not null, check `nightly_rate > 0`  |

`room`:

| Column         | Type          | Constraints                                    |
|----------------|---------------|------------------------------------------------|
| `id`           | `BIGINT`      | primary key, generated identity                |
| `room_number`  | `VARCHAR(10)` | not null, unique                               |
| `room_type_id` | `BIGINT`      | not null, foreign key to `room_type(id)`       |

Capacity and rate live on the room type only (decided during planning), so a rate change is one update.
Alternative: copy capacity and rate onto each room. Rejected: ten rooms share three types, copies would drift.

### 2. Seeded rooms (same migration)

| Room type | Capacity | Nightly rate (CHF) | Rooms        |
|-----------|----------|--------------------|--------------|
| Single    | 1        | 90.00              | 101–103      |
| Double    | 2        | 140.00             | 104–108      |
| Family    | 4        | 220.00             | 109–110      |

Later changes and their tests rely on these rooms.

### 3. Rate changes affect only new reservations

Reservations (from `add-reception-reservations`) copy the nightly rate when they are made. This change only states
the rule; nothing to build here.

### 4. Service and uniqueness

`RoomService` in `ch.martinelli.edu.hrs.room` (jOOQ, `@Transactional`) creates and updates room types and rooms.
Uniqueness is enforced by the unique constraints; the service translates `DuplicateKeyException` into the messages
from the spec. Alternative: check with a query first. Rejected: the constraint is needed anyway and a pre-check
races.

### 5. Views and staff layout

| Class           | Package                        | Route         | Access                    |
|-----------------|--------------------------------|---------------|---------------------------|
| `StaffLayout`   | `ch.martinelli.edu.hrs.core.ui`| layout        | –                         |
| `RoomTypeView`  | `ch.martinelli.edu.hrs.room.ui`| `room-types`  | `@RolesAllowed("MANAGER")`, `@AccessDeniedErrorRouter` |
| `RoomView`      | `ch.martinelli.edu.hrs.room.ui`| `rooms`       | `@RolesAllowed("MANAGER")`, `@AccessDeniedErrorRouter` |

- `StaffLayout` is an `AppLayout` with a `SideNav`; it adds an item only when `AccessAnnotationChecker` allows the
  current user to open that view, and shows the user's name with the logout button. `StaffStartView` moves into
  this layout (`@Route(value = "staff", layout = StaffLayout.class)`); its own logout button stays.
- Both views: a `Grid` with the list and a form below/beside it (`Binder` with bean validation messages) for create
  and edit. No delete button.
- Nightly rates are shown as `CHF 140.00`.

## Risks / Trade-offs

- [Manager changes a room type's capacity below guests of existing reservations] → Accepted: existing reservations
  keep their booking; the capacity check applies to new bookings and changes only.
- [Seeded rooms differ from the real hotel] → The manager edits them; the seed only makes the system usable.

## Migration Plan

New migration `V2`; no existing data to convert.
