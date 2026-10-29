---
id: 003-room-overview
unit: 002-front-desk
intent: 003-check-in-check-out-housekeeping
status: ready
priority: must
created: 2026-10-29T07:58:01Z
assigned_bolt: 006-front-desk
implemented: false
---

# Story: 003-room-overview

## User Story

**As a** receptionist
**I want** to see all rooms and their state today in one view
**So that** I know at a glance who arrives, who leaves and what is ready

## Acceptance Criteria

- [ ] **Given** the room overview, **When** I open it, **Then** I see one entry per room with number, type, cleaning status and today's state
- [ ] **Given** room 101 has a CHECKED_IN guest departing tomorrow, **When** I look at it, **Then** it shows OCCUPIED with guest name and booking number
- [ ] **Given** room 102's guest departs today and a new guest arrives today, **When** I look at it, **Then** it shows DEPARTING TODAY and ARRIVING TODAY with both guests
- [ ] **Given** room 103 is out of service, **When** I look at it, **Then** it shows OUT OF SERVICE
- [ ] **Given** a room with no stay today, **When** I look at it, **Then** it shows FREE
- [ ] **Given** an arriving or departing entry, **When** I choose check-in or check-out, **Then** the respective dialog opens
- [ ] **Given** a CONFIRMED reservation whose arrival date has passed without check-in, **When** I look at its room, **Then** it is flagged "Overdue arrival"

## Technical Notes

- One read query for today; no live push, a refresh button is enough.

## Dependencies

### Requires
- 001-check-in-guest, 002-check-out-guest

### Enables
- None

## Edge Cases

| Scenario | Expected Behavior |
|----------|-------------------|
| Tablet width | Entries wrap; no horizontal scrolling |

## Out of Scope

- Future days (use the reservation list)
