# Implementation Plan

Prerequisite: the foundation spec is implemented (room schema and seed, security, staff layout and menu, test base
classes with fixed clock, test accounts). No task here runs in parallel: each builds on the previous one and tasks
3.1 and 3.2 change the same view.

- [ ] 1. Store the cleaning status of every room
  - Main migration version 20 creates the cleaning status table: one row per room at most, status limited to Dirty and Clean, a declared foreign key to the room that is removed together with the room
  - The same migration gives every room existing at that point the status Clean
  - Done when `./mvnw compile` succeeds and generates the cleaning status jOOQ class; the stored content and constraints are proven by the service test in 2
  - _Requirements: 1.1, 1.2, 1.4, 5.2_

- [ ] 2. Provide the housekeeping service other features call
  - Cleaning status vocabulary with display labels, the room-status result type and the unknown-room error carrying the room id
  - Repository reads the status of all rooms and of one room from the room table with the status joined, treating a missing status as Dirty and ordering by room number; it saves a status by upsert
  - Service answers "is clean", marks a room dirty or clean (idempotent, unknown room rejected with nothing written), returns the cleaning list (dirty only) and all statuses
  - Service test: seeded rooms all Clean and listed in room-number order; mark dirty twice gives one list entry and "not clean"; mark clean empties the list and repeats without error; a test room 3000 without status row counts as Dirty until marked clean; unknown room 3999 is rejected by every method with the row count unchanged; a direct insert of an invalid status or of a second row for a room fails and leaves the status unchanged
  - Every test restores all rooms to Clean and removes room 3000 afterwards
  - Done when the service test passes and the repository is not visible outside the housekeeping package
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 3.2, 5.1, 5.2, 5.3, 5.4, 5.5_

- [ ] 3. Cleaning list for housekeepers
- [ ] 3.1 Show the cleaning list with refresh and role-based access
  - Staff page "Cleaning list" inside the staff layout, menu order 50, open to housekeepers and managers only
  - Shows the dirty rooms by room number in a full-width list with every row visible, the message "All rooms are clean" when none is dirty, and a Refresh action; reloads on every navigation
  - View test: two dirty rooms listed in order and no clean room; empty-state message; a room marked dirty while the list is open appears after Refresh and after navigating again; housekeeper and manager see the menu entry and open the page; a receptionist opening it by address gets the not-available view and has no menu entry; an anonymous visitor ends on the login page
  - Done when the view test cases for list, empty state, refresh and access pass
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 4.1, 4.2, 4.3, 5.1_

- [ ] 3.2 Mark a room clean in one step
  - Each row has a "Mark clean" action that marks the room clean without a confirmation question, shows "Room <number> is clean" and reloads the list
  - When the room no longer exists, shows "Room <number> no longer exists" and reloads the list without changing any status
  - View test: marking a dirty room clean shows the confirmation, removes the row, the room is clean and no dialog is open; a room already marked clean by someone else gives the same result; a deleted test room gives the no-longer-exists message and disappears from the list
  - Done when the mark-clean view test cases pass
  - _Requirements: 3.1, 3.2, 3.3_

- [ ] 4. Browser test of the housekeeper journey on tablet and desktop
  - With one room marked dirty through the service, log in as the test housekeeper and open the cleaning list at 768×1024: the room and its Mark clean action are visible and the page does not scroll horizontally
  - Mark the room clean and see the row disappear; repeat the layout check at 1280×800; restore statuses afterwards
  - Done when `./mvnw verify` passes with the new browser test counted in the failsafe reports
  - _Requirements: 2.5, 3.1, 4.1_
