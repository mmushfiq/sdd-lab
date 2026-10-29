---
id: 005-housekeeping
unit: 001-housekeeping
intent: 003-check-in-check-out-housekeeping
type: simple-construction-bolt
status: planned
stories:
  - 001-view-cleaning-list
  - 002-mark-room-cleaned
created: 2026-10-29T07:58:01Z
started: null
completed: null
current_stage: null
stages_completed: []

requires_bolts: [003-reservation]
enables_bolts: [006-front-desk]
requires_units: [001-room-catalog, 002-reservation]
blocks: true

complexity:
  avg_complexity: 1
  avg_uncertainty: 1
  max_dependencies: 2
  testing_scope: 1
---

# Bolt: 005-housekeeping

## Overview

Cleaning status on rooms and the housekeeping cleaning list.

## Objective

Housekeeping sees dirty rooms on a tablet and marks them clean.

## Stories Included

- **001-view-cleaning-list**: See which rooms need cleaning (Must)
- **002-mark-room-cleaned**: Mark a room as clean (Must)

## Bolt Type

**Type**: Simple Construction Bolt
**Definition**: `.specsmd/aidlc/templates/construction/bolt-types/simple-construction-bolt.md`

## Stages

- [ ] **1. plan**: Pending → implementation-plan.md
- [ ] **2. implement**: Pending → src/main/java/ch/martinelli/edu/hrs/housekeeping/
- [ ] **3. test**: Pending → test-walkthrough.md

## Dependencies

### Requires
- 003-reservation (intent 001 complete)

### Enables
- 006-front-desk

## Success Criteria

- [ ] All stories implemented
- [ ] All acceptance criteria met
- [ ] Tests passing
- [ ] Code reviewed
