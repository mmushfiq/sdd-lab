---
id: 004-guest-booking
unit: 001-guest-booking
intent: 002-online-booking
type: simple-construction-bolt
status: planned
stories:
  - 001-search-rooms-online
  - 002-book-room-online
  - 003-view-own-reservation
  - 004-cancel-own-reservation
created: 2026-10-29T07:58:01Z
started: null
completed: null
current_stage: null
stages_completed: []

requires_bolts: [003-reservation]
enables_bolts: [006-front-desk]
requires_units: [002-reservation]
blocks: true

complexity:
  avg_complexity: 2
  avg_uncertainty: 1
  max_dependencies: 2
  testing_scope: 3
---

# Bolt: 004-guest-booking

## Overview

Public guest area: search, book, view and cancel online.

## Objective

A guest can complete a booking in under 3 minutes and later view or cancel it with booking number and email.

## Stories Included

- **001-search-rooms-online**: Search free room types (Must)
- **002-book-room-online**: Book a room type (Must)
- **003-view-own-reservation**: Look up own reservation (Must)
- **004-cancel-own-reservation**: Cancel before the deadline (Must)

## Bolt Type

**Type**: Simple Construction Bolt
**Definition**: `.specsmd/aidlc/templates/construction/bolt-types/simple-construction-bolt.md`

## Stages

- [ ] **1. plan**: Pending → implementation-plan.md
- [ ] **2. implement**: Pending → src/main/java/ch/martinelli/edu/hrs/guestbooking/
- [ ] **3. test**: Pending → test-walkthrough.md

## Dependencies

### Requires
- 003-reservation (intent 001 complete)

### Enables
- 006-front-desk

## Success Criteria

- [ ] All stories implemented
- [ ] All acceptance criteria met
- [ ] Playwright IT: guest search → book → view → cancel
- [ ] Code reviewed
