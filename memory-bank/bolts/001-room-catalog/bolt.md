---
id: 001-room-catalog
unit: 001-room-catalog
intent: 001-rooms-and-reservations
type: simple-construction-bolt
status: complete
stories:
  - 001-manage-room-types
  - 002-delete-room-type
  - 003-manage-rooms
  - 004-set-room-out-of-service
created: '2026-10-29T07:20:41Z'
started: '2026-10-29T10:24:21Z'
completed: '2026-10-29T10:36:26Z'
current_stage: null
stages_completed:
  - name: plan
    completed: '2026-10-29T10:28:15Z'
    artifact: implementation-plan.md
  - name: implement
    completed: '2026-10-29T10:32:56Z'
    artifact: implementation-walkthrough.md
  - name: test
    completed: '2026-10-29T10:36:11Z'
    artifact: test-walkthrough.md
requires_bolts: []
enables_bolts:
  - 002-reservation
requires_units: []
blocks: false
complexity:
  avg_complexity: 1
  avg_uncertainty: 1
  max_dependencies: 1
  testing_scope: 1
---

# Bolt: 001-room-catalog

## Overview

First bolt of the project: room types and rooms, end to end (migration, jOOQ, service, Vaadin views, tests).

## Objective

The manager can maintain room types, rates and rooms, and mark rooms out of service. The ten placeholder rooms are seeded.

## Stories Included

- **001-manage-room-types**: Create, edit and list room types (Must)
- **002-delete-room-type**: Delete an unused room type (Must)
- **003-manage-rooms**: Create, edit, list and delete rooms (Must)
- **004-set-room-out-of-service**: Mark a room out of service (Must)

## Bolt Type

**Type**: Simple Construction Bolt
**Definition**: `.specsmd/aidlc/templates/construction/bolt-types/simple-construction-bolt.md`

## Stages

- [x] **1. plan**: Complete → implementation-plan.md
- [x] **2. implement**: Complete → src/main/java/ch/martinelli/edu/hrs/roomcatalog/, db/migration/V1__*.sql
- [x] **3. test**: Complete → test-walkthrough.md

## Dependencies

### Requires
- None (first bolt)

### Enables
- 002-reservation

## Success Criteria

- [x] All stories implemented
- [x] All acceptance criteria met
- [x] Tests passing (`./mvnw verify`)
- [ ] Code reviewed

## Notes

- Seed data uses placeholder rooms 101-110; confirm real rooms with the hotel.
