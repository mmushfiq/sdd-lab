# Brief: housekeeping

## Problem
Guests wait for rooms that are not yet clean because housekeeping and reception don't share the room's state.

## Current State
Rooms exist (foundation); there is no cleaning status.

## Desired Outcome
- Every room has a cleaning status: Dirty or Clean.
- The housekeeper sees the cleaning list (all dirty rooms) on a tablet and marks a room clean in one step.
- Other specs can ask whether a room is clean and mark a room dirty through this spec's service; a room marked dirty
  shows up on the cleaning list immediately.

## Approach
Cleaning status owned by a housekeeping service and repository (column or own table, decided in design); a
`CleaningListView` for the housekeeper role, laid out for tablet width.

## Scope
- **In**: cleaning status data and initial status of the ten rooms; cleaning list; mark clean; service API for
  "is clean" and "mark dirty".
- **Out**: check-out itself (reception), scheduling or assigning cleaning staff, cleaning history.

## Boundary Candidates
- Cleaning status ownership
- Housekeeper cleaning list UI

## Out of Boundary
- Deciding when a room becomes dirty (reception triggers it at check-out).

## Upstream / Downstream
- **Upstream**: foundation
- **Downstream**: reception

## Existing Spec Touchpoints
- **Extends**: none
- **Adjacent**: reception (calls mark dirty and is-clean), foundation (room table)

## Constraints
- Tablet-usable layout; only the housekeeper (and possibly manager) may open the cleaning list, decided in
  requirements.
- "Immediately" from the vision: the list reflects changes on the next load or refresh without manual sync; decide
  in design whether live push is needed.
