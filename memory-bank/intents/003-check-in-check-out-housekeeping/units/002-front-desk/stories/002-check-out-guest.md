---
id: 002-check-out-guest
unit: 002-front-desk
intent: 003-check-in-check-out-housekeeping
status: ready
priority: must
created: 2026-10-29T07:58:01Z
assigned_bolt: 006-front-desk
implemented: false
---

# Story: 002-check-out-guest

## User Story

**As a** receptionist
**I want** to check out a departing guest and see what they owe
**So that** the guest can pay and housekeeping knows to clean the room

## Acceptance Criteria

- [ ] **Given** a CHECKED_IN reservation, **When** I start check-out, **Then** I see the amount due (total price) before confirming
- [ ] **Given** I confirm, **When** check-out completes, **Then** the status is CHECKED_OUT with the check-out time and the room is DIRTY and on the cleaning list
- [ ] **Given** a guest leaves 2 days early, **When** I check them out, **Then** departure becomes today, the price is unchanged, and the room is free in availability search from tonight
- [ ] **Given** a reservation that is not CHECKED_IN, **When** I open it, **Then** check-out is not offered

## Technical Notes

- One transaction: reservation update + `markDirty` on the room.

## Dependencies

### Requires
- 001-check-in-guest

### Enables
- intent 005 reports

## Edge Cases

| Scenario | Expected Behavior |
|----------|-------------------|
| Guest stays past departure date | Check-out still allowed; departure unchanged |

## Out of Scope

- Recording payments, invoices
