---
id: 006-front-desk
unit: 002-front-desk
intent: 003-check-in-check-out-housekeeping
type: ddd-construction-bolt
status: planned
stories:
  - 001-check-in-guest
  - 002-check-out-guest
  - 003-room-overview
  - 004-include-stays-in-reservation-rules
created: 2026-10-29T07:58:01Z
started: null
completed: null
current_stage: null
stages_completed: []

requires_bolts: [005-housekeeping, 004-guest-booking]
enables_bolts: [007-staff-access, 009-management-reports]
requires_units: [001-housekeeping, 002-reservation]
blocks: true

complexity:
  avg_complexity: 2
  avg_uncertainty: 1
  max_dependencies: 2
  testing_scope: 3
---

# Bolt: 006-front-desk

## Overview

Stay lifecycle (check-in, check-out), room overview, and the new statuses in all existing reservation rules.

## Objective

Reception checks guests in (clean rooms only) and out in under a minute; the room overview shows today's state of every room.

## Stories Included

- **001-check-in-guest**: Check in to a clean room (Must)
- **002-check-out-guest**: Check out, room becomes dirty (Must)
- **003-room-overview**: All rooms' state today (Must)
- **004-include-stays-in-reservation-rules**: Respect new statuses in existing rules (Must)

## Bolt Type

**Type**: DDD Construction Bolt
**Definition**: `.specsmd/aidlc/templates/construction/bolt-types/ddd-construction-bolt.md`

## Stages

- [ ] **1. model**: Pending → ddd-01-domain-model.md
- [ ] **2. design**: Pending → ddd-02-technical-design.md
- [ ] **3. adr**: Pending (optional) → adr-*.md
- [ ] **4. implement**: Pending → src/main/java/ch/martinelli/edu/hrs/frontdesk/
- [ ] **5. test**: Pending → ddd-03-test-report.md

## Dependencies

### Requires
- 005-housekeeping
- 004-guest-booking (so story 004 can update the guest cancellation rule too)

### Enables
- 007-staff-access, 009-management-reports

## Success Criteria

- [ ] All stories implemented
- [ ] All acceptance criteria met; intent 001/002 tests still green
- [ ] Playwright IT: check-in → check-out → room on cleaning list
- [ ] Code reviewed
