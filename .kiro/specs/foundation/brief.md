# Brief: foundation

## Problem
Every other feature needs rooms to exist and staff to be identified by role. Today the app is an empty Spring Boot
and Vaadin skeleton with no schema, no login and no navigation.

## Current State
`HrsApplication` with Lumo styles, test base classes `HrsBrowserlessTest` and `PlaywrightIT`, no migrations, no
Spring Security dependency.

## Desired Outcome
- The database holds the room types and the hotel's ten rooms (number, room type, capacity, nightly rate).
- Staff members log in and land in a staff layout whose navigation shows only what their role may open.
- Guest-facing pages stay public; staff pages are refused to anonymous users and to the wrong role.
- Later specs can add views to the staff layout and menu without touching foundation code beyond a menu entry.

## Approach
First migrations create `room_type`, `room` and staff tables and seed reference data; Spring Security with Vaadin's
view access control; one staff layout with role-filtered navigation; a public landing route for guests.

## Scope
- **In**: room and room type tables plus seed data; staff member table with roles (receptionist, manager,
  housekeeper); login and logout; staff layout and navigation; public entry page; injected `Clock` bean.
- **Out**: editing rooms, room types or rates (management); maintaining staff accounts through the UI (not in the
  vision; staff are seeded); any reservation or cleaning data.

## Boundary Candidates
- Schema and seed data for rooms
- Authentication and role-based view access
- Staff layout and navigation shell

## Out of Boundary
- Cleaning status (housekeeping), reservations (reservation-core), room maintenance UI (management).

## Upstream / Downstream
- **Upstream**: none
- **Downstream**: all other specs

## Existing Spec Touchpoints
- **Extends**: none
- **Adjacent**: management (takes over changing room data), housekeeping (adds cleaning status to rooms)

## Constraints
- Passwords hashed; no credentials in main migrations beyond what seeded staff need (decide in design whether seeded
  staff get initial passwords and how they are changed).
- Staff layout must work on desktop and tablet.
