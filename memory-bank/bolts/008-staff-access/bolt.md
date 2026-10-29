---
id: 008-staff-access
unit: 001-staff-access
intent: 004-staff-login-and-roles
type: simple-construction-bolt
status: planned
stories:
  - 003-manage-staff-users
  - 004-reset-staff-password
created: 2026-10-29T07:58:01Z
started: null
completed: null
current_stage: null
stages_completed: []

requires_bolts: [007-staff-access]
enables_bolts: []
requires_units: []
blocks: true

complexity:
  avg_complexity: 2
  avg_uncertainty: 1
  max_dependencies: 2
  testing_scope: 2
---

# Bolt: 008-staff-access

## Overview

Staff user management by the Manager.

## Stories Included

- **003-manage-staff-users**: Create, edit, deactivate staff (Must)
- **004-reset-staff-password**: Manager resets a password (Must)

## Bolt Type

**Type**: Simple Construction Bolt
**Definition**: `.specsmd/aidlc/templates/construction/bolt-types/simple-construction-bolt.md`

## Stages

- [ ] **1. plan**: Pending → implementation-plan.md
- [ ] **2. implement**: Pending → src/main/java/ch/martinelli/edu/hrs/security/, staff/
- [ ] **3. test**: Pending → test-walkthrough.md

## Dependencies

### Requires
- 007-staff-access

### Enables
- None

## Success Criteria

- [ ] All stories implemented
- [ ] All acceptance criteria met
- [ ] All existing tests pass with login
- [ ] Code reviewed
