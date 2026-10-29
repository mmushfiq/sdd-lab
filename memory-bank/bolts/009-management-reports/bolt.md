---
id: 009-management-reports
unit: 001-management-reports
intent: 005-reports
type: ddd-construction-bolt
status: planned
stories:
  - 001-monthly-occupancy-report
  - 002-monthly-revenue-report
  - 003-report-by-room-type
created: 2026-10-29T07:58:01Z
started: null
completed: null
current_stage: null
stages_completed: []

requires_bolts: [006-front-desk, 007-staff-access]
enables_bolts: []
requires_units: [002-front-desk, 001-staff-access]
blocks: true

complexity:
  avg_complexity: 2
  avg_uncertainty: 1
  max_dependencies: 2
  testing_scope: 2
---

# Bolt: 009-management-reports

## Overview

Night-based monthly occupancy and revenue, with breakdown per room type, for the Manager.

## Objective

The Manager reads exact monthly figures in the app instead of a spreadsheet.

## Stories Included

- **001-monthly-occupancy-report**: Occupancy % for a month (Must)
- **002-monthly-revenue-report**: Revenue for a month (Must)
- **003-report-by-room-type**: Breakdown per room type (Must)

## Bolt Type

**Type**: DDD Construction Bolt
**Definition**: `.specsmd/aidlc/templates/construction/bolt-types/ddd-construction-bolt.md`

## Stages

- [ ] **1. model**: Pending → ddd-01-domain-model.md
- [ ] **2. design**: Pending → ddd-02-technical-design.md
- [ ] **3. adr**: Pending (optional) → adr-*.md
- [ ] **4. implement**: Pending → src/main/java/ch/martinelli/edu/hrs/report/
- [ ] **5. test**: Pending → ddd-03-test-report.md

## Dependencies

### Requires
- 006-front-desk, 007-staff-access

### Enables
- None (last planned bolt)

## Success Criteria

- [ ] All stories implemented
- [ ] Figures match a hand-calculated test month
- [ ] Tests passing
- [ ] Code reviewed
