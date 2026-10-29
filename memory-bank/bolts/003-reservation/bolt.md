---
id: 003-reservation
unit: 002-reservation
intent: 001-rooms-and-reservations
type: ddd-construction-bolt
status: complete
stories:
  - 004-change-reservation
  - 005-cancel-reservation
  - 006-protect-rooms-with-reservations
  - 007-anonymize-guest-data
created: '2026-10-29T07:20:41Z'
started: '2026-10-29T11:08:26Z'
completed: '2026-10-29T11:58:03Z'
current_stage: null
stages_completed:
  - name: model
    completed: '2026-10-29T11:13:14Z'
    artifact: ddd-01-domain-model.md
  - name: design
    completed: '2026-10-29T11:14:58Z'
    artifact: ddd-02-technical-design.md
  - name: adr
    completed: '2026-10-29T11:48:59Z'
    artifact: adr-003-anonymize-guest-data-in-place.md (+ ADR-001 extended)
  - name: implement
    completed: '2026-10-29T11:53:54Z'
    artifact: src/main/java/ch/martinelli/edu/hrs/reservation/, roomcatalog/RoomCatalogService.java
  - name: test
    completed: '2026-10-29T11:58:03Z'
    artifact: ddd-03-test-report.md
requires_bolts:
  - 002-reservation
enables_bolts:
  - 004-guest-booking
  - 005-housekeeping
requires_units:
  - 001-room-catalog
blocks: true
complexity:
  avg_complexity: 2
  avg_uncertainty: 1
  max_dependencies: 2
  testing_scope: 2
---

# Bolt: 003-reservation

## Overview

Reservation lifecycle after booking (change, cancel), protection of booked rooms, and GDPR anonymization.

## Objective

Reception can change and cancel reservations; booked rooms can't be deleted or blocked; guest data can be anonymized on request.

## Stories Included

- **004-change-reservation**: Change dates, room, guests or guest data (Must)
- **005-cancel-reservation**: Cancel a confirmed reservation (Must)
- **006-protect-rooms-with-reservations**: Block delete / out-of-service for booked rooms (Must)
- **007-anonymize-guest-data**: Anonymize guest data on request (Should)

## Bolt Type

**Type**: DDD Construction Bolt
**Definition**: `.specsmd/aidlc/templates/construction/bolt-types/ddd-construction-bolt.md`

## Stages

- [x] **1. model**: Complete → ddd-01-domain-model.md
- [x] **2. design**: Complete → ddd-02-technical-design.md
- [x] **3. adr**: Complete (ADR-003, ADR-001 extended) → adr-*.md
- [x] **4. implement**: Complete → src/main/java/ch/martinelli/edu/hrs/reservation/
- [x] **5. test**: Complete → ddd-03-test-report.md

## Dependencies

### Requires
- 002-reservation

### Enables
- Intent 001 complete; 004-guest-booking, 005-housekeeping

## Success Criteria

- [x] All stories implemented
- [x] All acceptance criteria met
- [x] Tests passing (`./mvnw verify`)
- [ ] Code reviewed
