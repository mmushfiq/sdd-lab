---
id: 001-manage-room-types
unit: 001-room-catalog
intent: 001-rooms-and-reservations
status: ready
priority: must
created: 2026-10-29T07:20:41Z
assigned_bolt: 001-room-catalog
implemented: false
---

# Story: 001-manage-room-types

## User Story

**As a** manager
**I want** to create, edit and list room types with capacity and nightly rate
**So that** rooms can be categorized and priced

## Acceptance Criteria

- [ ] **Given** the room type view, **When** I open it, **Then** I see all room types with name, capacity and nightly rate, sorted by name
- [ ] **Given** valid data (name "Double", capacity 2, rate 150.00), **When** I save, **Then** the room type appears in the list
- [ ] **Given** an existing room type "Double", **When** I save another type named "double", **Then** I see the field error "Name already exists" and nothing is saved
- [ ] **Given** capacity 0 or 11, **When** I save, **Then** I see a capacity validation error
- [ ] **Given** a rate of 0, a negative rate, or more than two decimals, **When** I save, **Then** I see a rate validation error
- [ ] **Given** an existing room type, **When** I change its rate and save, **Then** the list shows the new rate

## Technical Notes

- Migration creates `room_type` (unique index on `lower(name)`) and seeds the placeholder types Single (1, 120.00), Double (2, 150.00), Family (4, 220.00).
- Rate as DECIMAL(10,2), `BigDecimal` in Java.

## Dependencies

### Requires
- None (first story)

### Enables
- 002-delete-room-type, 003-manage-rooms

## Edge Cases

| Scenario | Expected Behavior |
|----------|-------------------|
| Name with leading/trailing spaces | Trimmed before validation and save |
| Empty name | Required-field error |

## Out of Scope

- Effect of rate changes on existing reservations (covered in 002-reservation: price is stored on the reservation)
