---
unit: 001-housekeeping
intent: 003-check-in-check-out-housekeeping
phase: inception
status: ready
unit_type: backend
default_bolt_type: simple-construction-bolt
created: 2026-10-29T07:58:01Z
updated: 2026-10-29T07:58:01Z
---

# Unit Brief: Housekeeping

## Purpose

Track whether each room is clean and give housekeeping a tablet list of rooms to clean.

## Scope

### In Scope
- Cleaning status CLEAN/DIRTY on rooms (+ dirty-since time)
- Cleaning list and "mark cleaned"
- Showing cleaning status in the intent-001 room list

### Out of Scope
- Setting rooms DIRTY (done by check-out in 002-front-desk)
- Daily cleaning of occupied rooms, staff assignment

---

## Assigned Requirements

| FR | Requirement | Priority |
|----|-------------|----------|
| FR-1 | Room cleaning status | Must |
| FR-2 | Cleaning list | Must |
| FR-3 | Mark room cleaned | Must |

---

## Domain Concepts

### Key Entities
| Entity | Description | Attributes |
|--------|-------------|------------|
| Room (extended) | Room from intent 001 | + cleaningStatus (CLEAN, DIRTY), dirtySince |

### Key Operations
| Operation | Description | Inputs | Outputs |
|-----------|-------------|--------|---------|
| findRoomsToClean | Dirty rooms, arrivals today first | today | list |
| markCleaned | DIRTY → CLEAN | roomId | ok or "already clean" |
| markDirty | CLEAN → DIRTY (used by check-out) | roomId, time | ok |

---

## Story Summary

- **Total Stories**: 2
- **Must Have**: 2
- **Should Have**: 0
- **Could Have**: 0

### Stories

- [ ] **001-view-cleaning-list**: See which rooms need cleaning - Must - Planned
- [ ] **002-mark-room-cleaned**: Mark a room as clean - Must - Planned

---

## Dependencies

### Depends On
| Unit | Reason |
|------|--------|
| intent 001 / 001-room-catalog | Rooms |
| intent 001 / 002-reservation | "Arrival today" flag |

### Depended By
| Unit | Reason |
|------|--------|
| 002-front-desk | Check-in requires CLEAN; check-out calls markDirty |

### External Dependencies
| System | Purpose | Risk |
|--------|---------|------|
| - | None | - |

---

## Technical Context

### Suggested Technology
Vaadin Flow view with large buttons (tablet), `Clock` for "today". Package `ch.martinelli.edu.hrs.housekeeping`.

### Integration Points
| Integration | Type | Protocol |
|-------------|------|----------|
| room table | DB | in-process |

### Data Storage
| Data | Type | Volume | Retention |
|------|------|--------|-----------|
| room.cleaning_status, room.dirty_since | SQL | 10 rows | current state only |

---

## Constraints

- markCleaned uses a conditional update (`WHERE cleaning_status = 'DIRTY'`) so two tablets can't conflict.

---

## Success Criteria

### Functional
- [ ] FR-1 to FR-3 acceptance criteria pass

### Non-Functional
- [ ] Usable on a tablet (touch targets ≥ 44 px)

### Quality
- [ ] Every acceptance criterion has a test
- [ ] Code reviewed and approved

---

## Bolt Suggestions

| Bolt | Type | Stories | Objective |
|------|------|---------|-----------|
| 005-housekeeping | Simple | 001, 002 | Cleaning status and list |

---

## Notes

- Until check-out exists (bolt 006), tests set rooms DIRTY through the service's markDirty.
