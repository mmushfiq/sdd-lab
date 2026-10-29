---
intent: 004-staff-login-and-roles
created: 2026-10-29T07:58:01Z
completed: 2026-10-29T07:58:01Z
status: complete
---

# Inception Log: 004-staff-login-and-roles

## Overview

**Intent**: Staff login, role-based access, staff user management; guest pages stay public.
**Type**: green-field
**Created**: 2026-10-29T07:58:01Z
**Source**: `docs/hotel-requirements.md`

## Artifacts Created

| Artifact | Status | File |
|----------|--------|------|
| Requirements | ✅ | requirements.md |
| System Context | ✅ | system-context.md |
| Units | ✅ | units.md, units/*/unit-brief.md |
| Stories | ✅ | units/*/stories/*.md (4) |
| Bolt Plan | ✅ | memory-bank/bolts/007-staff-access, 008-staff-access |

## Summary

| Metric | Count |
|--------|-------|
| Functional Requirements | 5 |
| Units | 1 |
| Stories | 4 |
| Bolts Planned | 2 |

## Units Breakdown

| Unit | Stories | Bolts | Priority |
|------|---------|-------|----------|
| 001-staff-access | 4 | 2 | Must |

## Decision Log

| Date | Decision | Rationale | Approved |
|------|----------|-----------|----------|
| 2026-10-29T07:58:01Z | Users in the database, managed by the Manager; initial manager from `hrs.initial-manager-password`; passwords ≥ 10 chars, BCrypt | Default | Yes |
| 2026-10-29T07:58:01Z | Housekeeping: cleaning list; Receptionist: reservations, overview, check-in/out, anonymize; Manager: everything | Least privilege | Yes |
| 2026-10-29T07:58:01Z | Protect all views in one bolt (007) | App never half-protected | Yes |

## Scope Changes

| Date | Change | Reason | Impact |
|------|--------|--------|--------|

## Ready for Construction

**Checklist**:
- [x] All requirements documented
- [x] System context defined
- [x] Units decomposed
- [x] Stories created for all units
- [x] Bolts planned
- [x] Human review complete (decisions approved as defaults on 2026-10-29)

## Next Steps

Start after bolt 006: `/specsmd-construction-agent --unit="001-staff-access" --bolt-id="007-staff-access"`
