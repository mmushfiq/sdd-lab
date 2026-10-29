---
unit: 002-front-desk
intent: 003-check-in-check-out-housekeeping
phase: inception
status: ready
unit_type: backend
default_bolt_type: ddd-construction-bolt
created: 2026-10-29T07:58:01Z
updated: 2026-10-29T07:58:01Z
---

# Unit Brief: Front Desk

## Purpose

Let reception check guests in and out quickly, enforce the clean-room rule, and show the state of all rooms today.

## Scope

### In Scope
- Check-in, check-out (incl. early check-out)
- Room overview for today
- Extending the reservation lifecycle with CHECKED_IN / CHECKED_OUT and updating intent-001/002 rules

### Out of Scope
- Payment recording, invoices
- Automatic no-show handling

---

## Assigned Requirements

| FR | Requirement | Priority |
|----|-------------|----------|
| FR-4 | Check in a guest | Must |
| FR-5 | Check out a guest | Must |
| FR-6 | Room overview | Must |
| FR-7 | Stays in the reservation rules | Must |

---

## Domain Concepts

### Key Entities
| Entity | Description | Attributes |
|--------|-------------|------------|
| Reservation (extended) | Lifecycle CONFIRMED → CHECKED_IN → CHECKED_OUT; CONFIRMED → CANCELLED | + checkedInAt, checkedOutAt |
| RoomDayState (read model) | A room's state today | room, cleaningStatus, state(s), guest name, booking number |

### Key Operations
| Operation | Description | Inputs | Outputs |
|-----------|-------------|--------|---------|
| checkIn | CONFIRMED → CHECKED_IN if date window ok and room CLEAN | bookingNumber, now | updated reservation or exception |
| checkOut | CHECKED_IN → CHECKED_OUT, room DIRTY, departure = today if early | bookingNumber, now | amount due, updated reservation |
| roomOverview | Today's state of all rooms | today | list of RoomDayState |

---

## Story Summary

- **Total Stories**: 4
- **Must Have**: 4
- **Should Have**: 0
- **Could Have**: 0

### Stories

- [ ] **001-check-in-guest**: Check in a guest to a clean room - Must - Planned
- [ ] **002-check-out-guest**: Check out a guest, room becomes dirty - Must - Planned
- [ ] **003-room-overview**: See all rooms' state today - Must - Planned
- [ ] **004-include-stays-in-reservation-rules**: Respect new statuses in existing rules - Must - Planned

---

## Dependencies

### Depends On
| Unit | Reason |
|------|--------|
| 001-housekeeping | Cleaning status, markDirty |
| intent 001 / 002-reservation | Reservation aggregate and rules |

### Depended By
| Unit | Reason |
|------|--------|
| intent 005 / 001-management-reports | Reports count CHECKED_IN / CHECKED_OUT stays |

### External Dependencies
| System | Purpose | Risk |
|--------|---------|------|
| - | None | - |

---

## Technical Context

### Suggested Technology
Extend the reservation aggregate (status transitions as methods), `@Transactional` services, injected `Clock`. Package `ch.martinelli.edu.hrs.frontdesk`.

### Integration Points
| Integration | Type | Protocol |
|-------------|------|----------|
| ReservationService, HousekeepingService | Java service calls | in-process |

### Data Storage
| Data | Type | Volume | Retention |
|------|------|--------|-----------|
| reservation.checked_in_at, checked_out_at | SQL | per reservation | as reservation |

---

## Constraints

- Check-in and check-out run in one transaction with the room's cleaning status update.
- The overlap query of intent 001 changes from `status = 'CONFIRMED'` to `status <> 'CANCELLED'`; all intent-001 and 002 tests must still pass.

---

## Success Criteria

### Functional
- [ ] FR-4 to FR-7 acceptance criteria pass; intent 001/002 tests still green

### Non-Functional
- [ ] Zero check-ins to DIRTY rooms (service-level test)
- [ ] Check-in/out flows in under 1 minute (Playwright IT)

### Quality
- [ ] Every acceptance criterion has a test
- [ ] Code reviewed and approved

---

## Bolt Suggestions

| Bolt | Type | Stories | Objective |
|------|------|---------|-----------|
| 006-front-desk | DDD | 001-004 | Stay lifecycle and front-desk views |

---

## Notes

- Early check-out changes the departure date; keep the stored price.
