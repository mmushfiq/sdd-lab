---
intent: 003-check-in-check-out-housekeeping
phase: inception
status: inception-complete
created: 2026-10-29T07:58:01Z
updated: 2026-10-29T07:58:01Z
---

# Requirements: Check-in, Check-out and Housekeeping

## Intent Overview

Reception checks guests in and out and sees the state of every room at a glance; housekeeping sees which rooms need cleaning and marks them clean. A guest can only be checked in to a clean room, and a room appears on the cleaning list immediately after check-out.

Source: `docs/hotel-requirements.md`.

## Business Goals

| Goal | Success Metric | Priority |
|------|----------------|----------|
| No guest in an unclean room | Zero check-ins to rooms marked DIRTY | Must |
| Fast front desk | Check-in or check-out in under 1 minute | Must |
| Housekeeping knows what to clean | Room on the cleaning list immediately after check-out | Must |

---

## Functional Requirements

### FR-1: Room cleaning status
- **Description**: Every room has a cleaning status CLEAN or DIRTY.
- **Acceptance Criteria**:
  - Existing and new rooms start as CLEAN.
  - Check-out sets the room to DIRTY; marking cleaned sets it to CLEAN. No other transitions.
  - The cleaning status is shown in the room list (intent 001) and the room overview (FR-6).
- **Priority**: Must
- **Related Stories**: 001-housekeeping/001-view-cleaning-list

### FR-2: Cleaning list
- **Description**: Housekeeping sees all DIRTY rooms, usable on a tablet.
- **Acceptance Criteria**:
  - Lists every DIRTY room with room number, room type, time it became dirty, and whether a guest arrives in it today.
  - Rooms with an arrival today are listed first, then by time it became dirty.
  - A room appears on the list immediately after check-out (on next refresh/open, no manual step).
  - Empty list shows "All rooms are clean".
- **Priority**: Must
- **Related Stories**: 001-housekeeping/001-view-cleaning-list

### FR-3: Mark room cleaned
- **Description**: Housekeeping marks a DIRTY room as CLEAN.
- **Acceptance Criteria**:
  - One tap/click per room, large enough for a tablet.
  - The room disappears from the cleaning list and becomes CLEAN.
  - Marking an already CLEAN room (e.g. done on another tablet) shows "Room is already clean" and changes nothing.
- **Priority**: Must
- **Related Stories**: 001-housekeeping/002-mark-room-cleaned

### FR-4: Check in a guest
- **Description**: Reception checks in a CONFIRMED reservation.
- **Acceptance Criteria**:
  - Allowed from the arrival date until the day before departure (hotel time zone).
  - Allowed only if the room is CLEAN; otherwise "Room {n} is not clean yet" and nothing changes.
  - Sets status CHECKED_IN and records the check-in time.
  - A CONFIRMED reservation whose arrival date has passed without check-in stays CONFIRMED (no-show) until reception cancels it.
- **Priority**: Must
- **Related Stories**: 002-front-desk/001-check-in-guest

### FR-5: Check out a guest
- **Description**: Reception checks out a CHECKED_IN reservation on any day of the stay.
- **Acceptance Criteria**:
  - Shows the amount due (the reservation's total price) before confirming; payment is not recorded.
  - Sets status CHECKED_OUT, records the check-out time and sets the room DIRTY.
  - Early check-out: departure date becomes today, price unchanged, the remaining nights are free for new bookings.
- **Priority**: Must
- **Related Stories**: 002-front-desk/002-check-out-guest

### FR-6: Room overview
- **Description**: Reception sees all rooms with today's state at a glance.
- **Acceptance Criteria**:
  - One tile/row per room: room number, type, cleaning status, and today's state: OUT OF SERVICE, OCCUPIED, DEPARTING TODAY, ARRIVING TODAY or FREE (a room can be both DEPARTING and ARRIVING).
  - Occupied, departing and arriving rooms show the guest name and booking number.
  - Check-in and check-out can be started from the overview.
- **Priority**: Must
- **Related Stories**: 002-front-desk/003-room-overview

### FR-7: Stays in the reservation rules
- **Description**: The new statuses are respected by the rules of intents 001 and 002.
- **Acceptance Criteria**:
  - Availability and double-booking checks count every reservation that is not CANCELLED (CONFIRMED, CHECKED_IN, CHECKED_OUT).
  - Change and cancel (reception and guest) remain possible only for CONFIRMED reservations.
  - A room with a CHECKED_IN reservation cannot be set out of service or deleted.
  - Anonymization is allowed for CHECKED_OUT and CANCELLED reservations.
- **Priority**: Must
- **Related Stories**: 002-front-desk/004-include-stays-in-reservation-rules

---

## Non-Functional Requirements

### Performance
| Requirement | Metric | Target |
|-------------|--------|--------|
| Overview and cleaning list | Load time | < 1 s |

### Usability
| Requirement | Metric | Target |
|-------------|--------|--------|
| Check-in / check-out | Time at the desk | < 1 minute |
| Cleaning list | Device | Tablet, touch targets ≥ 44 px |

### Reliability
| Requirement | Metric | Target |
|-------------|--------|--------|
| Clean-room rule | Check-ins to DIRTY rooms | 0, enforced in the service |

---

## Constraints

### Technical Constraints

**Project-wide standards**: loaded from memory-bank standards folder by Construction Agent

**Intent-specific constraints**:
- "Today" uses the hotel time zone (Europe/Zurich) through an injected `Clock`.
- No push updates needed; lists refresh when opened or by a refresh button.

### Business Constraints
- Payment is settled at the hotel and not recorded in the system.

---

## Assumptions

| Assumption | Risk if Invalid | Mitigation |
|------------|-----------------|------------|
| Rooms only get dirty at check-out | Occupied rooms' daily cleaning not tracked | Add daily cleaning later |
| No-shows are cancelled manually | Rooms blocked by no-shows | Room overview shows overdue arrivals |

---

## Open Questions

| Question | Owner | Due Date | Resolution |
|----------|-------|----------|------------|
| Is daily cleaning of occupied rooms needed in the system? | Housekeeping | After go-live | Pending |
