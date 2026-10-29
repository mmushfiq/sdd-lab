---
id: 002-reservation
unit: 002-reservation
intent: 001-rooms-and-reservations
type: ddd-construction-bolt
status: complete
stories:
  - 001-search-availability
  - 002-create-reservation
  - 003-find-reservations
created: '2026-10-29T07:20:41Z'
started: '2026-10-29T10:43:22Z'
completed: '2026-10-29T11:05:55Z'
current_stage: null
stages_completed:
  - name: model
    completed: '2026-10-29T10:46:42Z'
    artifact: ddd-01-domain-model.md
  - name: design
    completed: '2026-10-29T10:47:34Z'
    artifact: ddd-02-technical-design.md
  - name: adr
    completed: '2026-10-29T10:52:40Z'
    artifact: adr-001-room-row-lock-against-double-booking.md, adr-002-direct-reads-of-room-catalog-tables.md
  - name: implement
    completed: '2026-10-29T10:56:17Z'
    artifact: src/main/java/ch/martinelli/edu/hrs/reservation/
  - name: test
    completed: '2026-10-29T11:05:55Z'
    artifact: ddd-03-test-report.md
requires_bolts:
  - 001-room-catalog
enables_bolts:
  - 003-reservation
requires_units:
  - 001-room-catalog
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

- [x] **1. model**: Complete → ddd-01-domain-model.md
- [x] **2. design**: Complete → ddd-02-technical-design.md
- [x] **3. adr**: Complete (2 ADRs) → adr-*.md (candidate: room-row locking for double-booking protection)
- [x] **4. implement**: Complete → src/main/java/ch/martinelli/edu/hrs/reservation/
- [x] **5. test**: Complete → ddd-03-test-report.md

## Dependencies

### Requires
- 001-room-catalog

### Enables
- 003-reservation

## Success Criteria

- [x] All stories implemented
- [x] All acceptance criteria met, incl. concurrency test
- [x] Playwright IT: search → book → see booking number
- [ ] Code reviewed
