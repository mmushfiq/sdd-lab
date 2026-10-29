---
id: 003-reservation
unit: 002-reservation
intent: 001-rooms-and-reservations
type: ddd-construction-bolt
status: planned
stories:
  - 004-change-reservation
  - 005-cancel-reservation
  - 006-protect-rooms-with-reservations
  - 007-anonymize-guest-data
created: 2026-10-29T07:20:41Z
started: null
completed: null
current_stage: null
stages_completed: []

requires_bolts: [002-reservation]
enables_bolts: []
requires_units: [001-room-catalog]
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

- [ ] **1. model**: Pending → ddd-01-domain-model.md
- [ ] **2. design**: Pending → ddd-02-technical-design.md
- [ ] **3. adr**: Pending (optional) → adr-*.md
- [ ] **4. implement**: Pending → src/main/java/ch/martinelli/edu/hrs/reservation/
- [ ] **5. test**: Pending → ddd-03-test-report.md

## Dependencies

### Requires
- 002-reservation

### Enables
- Intent 001 complete

## Success Criteria

- [ ] All stories implemented
- [ ] All acceptance criteria met
- [ ] Tests passing (`./mvnw verify`)
- [ ] Code reviewed
