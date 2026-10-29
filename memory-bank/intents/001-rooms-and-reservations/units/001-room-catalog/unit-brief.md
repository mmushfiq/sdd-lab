---
unit: 001-room-catalog
intent: 001-rooms-and-reservations
phase: inception
status: complete
unit_type: backend
default_bolt_type: simple-construction-bolt
created: '2026-10-29T07:20:41Z'
updated: '2026-10-29T07:20:41Z'
---

# Unit Brief: Room Catalog

## Purpose

Keep the hotel's rooms, their room types and nightly rates in the system, so that availability search and reservations have a reliable list of bookable rooms.

## Scope

### In Scope
- Create, edit, list and delete room types (name, capacity, nightly rate)
- Create, edit, list and delete rooms (room number, room type)
- Out-of-service flag on rooms
- Seed data for the hotel's ten rooms

### Out of Scope
- Rules that depend on reservations (delete/out-of-service blocked by reservations) → `002-reservation` story 006
- Seasonal or weekend rates
- Cleaning status (intent 003)

---

## Assigned Requirements

| FR | Requirement | Priority |
|----|-------------|----------|
| FR-1 | Manage room types | Must |
| FR-2 | Manage rooms (except the reservation-dependent rules) | Must |

---

## Domain Concepts

### Key Entities
| Entity | Description | Attributes |
|--------|-------------|------------|
| RoomType | A category of room with a price | id, name (unique, case-insensitive), capacity (1-10), nightlyRate (CHF, 2 decimals) |
| Room | A physical room that can be booked | id, number (unique), roomType, outOfService (boolean) |

### Key Operations
| Operation | Description | Inputs | Outputs |
|-----------|-------------|--------|---------|
| saveRoomType | Create or update a room type | name, capacity, nightlyRate | RoomType or validation error |
| deleteRoomType | Delete an unused room type | id | ok, or error "used by rooms" |
| saveRoom | Create or update a room | number, roomTypeId, outOfService | Room or validation error |
| deleteRoom | Delete a room | id | ok |
| findRooms / findRoomTypes | List for the views | - | lists |

---

## Story Summary

- **Total Stories**: 4
- **Must Have**: 4
- **Should Have**: 0
- **Could Have**: 0

### Stories

- [ ] **001-manage-room-types**: Create, edit and list room types - Must - Planned
- [ ] **002-delete-room-type**: Delete an unused room type - Must - Planned
- [ ] **003-manage-rooms**: Create, edit, list and delete rooms - Must - Planned
- [ ] **004-set-room-out-of-service**: Mark a room as out of service - Must - Planned

---

## Dependencies

### Depends On
| Unit | Reason |
|------|--------|
| - | Foundation unit |

### Depended By
| Unit | Reason |
|------|--------|
| 002-reservation | Reservations are made for rooms and priced from room types |

### External Dependencies
| System | Purpose | Risk |
|--------|---------|------|
| - | None | - |

---

## Technical Context

### Suggested Technology
Per `standards/`: Flyway migration, jOOQ generated records, Spring `@Service`, Vaadin Flow views with Grid + form and `Binder`. Package `ch.martinelli.edu.hrs.roomcatalog`.

### Integration Points
| Integration | Type | Protocol |
|-------------|------|----------|
| 002-reservation | DB (shared schema) + Java service calls | in-process |

### Data Storage
| Data | Type | Volume | Retention |
|------|------|--------|-----------|
| room_type, room | SQL (H2) | ~3 types, 10 rooms | Permanent |

---

## Constraints

- Nightly rate stored as DECIMAL(10,2) in CHF.
- Uniqueness of room type name and room number enforced by DB unique constraints and shown as a field error in the form.

---

## Success Criteria

### Functional
- [ ] Manager can maintain room types and rooms with all FR-1/FR-2 validations
- [ ] Out-of-service rooms are marked and visible in the room list

### Non-Functional
- [ ] Views usable on desktop and tablet
- [ ] Lists and saves respond in under 1 s

### Quality
- [ ] Every acceptance criterion has a Browserless test
- [ ] Code reviewed and approved

---

## Bolt Suggestions

| Bolt | Type | Stories | Objective |
|------|------|---------|-----------|
| 001-room-catalog | Simple | 001-004 | Room types and rooms, end to end |

---

## Notes

- Seed the ten rooms in a migration so reservations can be tried immediately. The actual room numbers and types should be confirmed by the hotel; placeholder: 3 types (Single, Double, Family), rooms 101-110.
