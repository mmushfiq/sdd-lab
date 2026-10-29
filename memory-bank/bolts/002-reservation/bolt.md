---
id: 002-reservation
unit: 002-reservation
intent: 001-rooms-and-reservations
type: ddd-construction-bolt
status: planned
stories:
  - 001-search-availability
  - 002-create-reservation
  - 003-find-reservations
created: 2026-10-29T07:20:41Z
started: null
completed: null
current_stage: null
stages_completed: []

requires_bolts: [001-room-catalog]
enables_bolts: [003-reservation]
requires_units: [001-room-catalog]
blocks: true

complexity:
  avg_complexity: 2
  avg_uncertainty: 1
  max_dependencies: 2
  testing_scope: 3
---

# Bolt: 002-reservation

## Overview

Core reservation domain: the stay period and overlap rule, availability search, booking with double-booking protection, and the reservation list.

## Objective

Reception can search free rooms and book them for phone and walk-in guests, with zero double bookings even under concurrent saves.

## Stories Included

- **001-search-availability**: Search free rooms for dates and guests (Must)
- **002-create-reservation**: Book a room for a guest (Must)
- **003-find-reservations**: List and filter reservations (Must)

## Bolt Type

**Type**: DDD Construction Bolt
**Definition**: `.specsmd/aidlc/templates/construction/bolt-types/ddd-construction-bolt.md`

## Stages

- [ ] **1. model**: Pending → ddd-01-domain-model.md
- [ ] **2. design**: Pending → ddd-02-technical-design.md
- [ ] **3. adr**: Pending (optional) → adr-*.md (candidate: room-row locking for double-booking protection)
- [ ] **4. implement**: Pending → src/main/java/ch/martinelli/edu/hrs/reservation/
- [ ] **5. test**: Pending → ddd-03-test-report.md

## Dependencies

### Requires
- 001-room-catalog

### Enables
- 003-reservation

## Success Criteria

- [ ] All stories implemented
- [ ] All acceptance criteria met, incl. concurrency test
- [ ] Playwright IT: search → book → see booking number
- [ ] Code reviewed
