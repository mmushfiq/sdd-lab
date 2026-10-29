---
id: 007-staff-access
unit: 001-staff-access
intent: 004-staff-login-and-roles
type: simple-construction-bolt
status: planned
stories:
  - 001-log-in-and-out
  - 002-restrict-views-by-role
created: 2026-10-29T07:58:01Z
started: null
completed: null
current_stage: null
stages_completed: []

requires_bolts: [006-front-desk]
enables_bolts: [008-staff-access, 009-management-reports]
requires_units: []
blocks: true

complexity:
  avg_complexity: 2
  avg_uncertainty: 2
  max_dependencies: 2
  testing_scope: 2
---

# Bolt: 007-staff-access

## Overview

Login, initial manager account, and role rules on every view and service. Done in one bolt so the app is never half-protected.

## Stories Included

- **001-log-in-and-out**: Log in, log out, initial manager (Must)
- **002-restrict-views-by-role**: Role rules on views and services (Must)

## Bolt Type

**Type**: Simple Construction Bolt
**Definition**: `.specsmd/aidlc/templates/construction/bolt-types/simple-construction-bolt.md`

## Stages

- [ ] **1. plan**: Pending → implementation-plan.md
- [ ] **2. implement**: Pending → src/main/java/ch/martinelli/edu/hrs/security/, staff/
- [ ] **3. test**: Pending → test-walkthrough.md

## Dependencies

### Requires
- 006-front-desk

### Enables
- 008-staff-access, 009-management-reports

## Success Criteria

- [ ] All stories implemented
- [ ] All acceptance criteria met
- [ ] All existing tests pass with login
- [ ] Code reviewed
