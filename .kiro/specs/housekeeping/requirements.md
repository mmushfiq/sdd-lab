# Requirements Document

## Project Description (Input)
Guests wait for rooms that are not yet clean because housekeeping and reception don't share the room's state. Rooms
exist (foundation), but there is no cleaning status. Every room gets a cleaning status, Dirty or Clean. The housekeeper
sees the cleaning list (all dirty rooms) on a tablet and marks a room clean in one step. Other features can ask whether
a room is clean and mark a room dirty; a room marked dirty shows up on the cleaning list immediately. In scope: cleaning
status data and initial status of the ten rooms; cleaning list; mark clean; the "is clean" and "mark dirty" capability
for other features. Out of scope: check-out itself (reception), scheduling or assigning cleaning staff, cleaning
history. Deciding when a room becomes dirty is out of boundary (reception triggers it at check-out). Constraints:
tablet-usable layout; only the housekeeper (and possibly the manager) may open the cleaning list; "immediately" means
the list reflects changes on the next load or refresh without manual sync. Source: `.kiro/specs/housekeeping/brief.md`.

## Introduction
Housekeeping gives every room of the Hotel Reservation System (HRS) a cleaning status, Dirty or Clean, shows the
housekeeper the cleaning list of all dirty rooms on a tablet, and lets the housekeeper mark a room clean in one step.
It is the single source of a room's cleaning status: reception asks it whether a room is clean before check-in and
tells it that a room is dirty after check-out.

## Boundary Context
- **In scope**: the cleaning status of every room and its initial value; the cleaning list; marking a room clean;
  answering whether a room is clean; marking a room dirty on request of another feature; giving another feature the
  cleaning status of all rooms.
- **Out of scope**: check-in and check-out and the decision when a room becomes dirty (reception); a page or action for
  staff to mark a room dirty by hand; scheduling or assigning cleaning work to housekeepers; cleaning history or
  timestamps of past changes; creating, changing or deleting rooms (foundation, management).
- **Adjacent expectations**: foundation provides the ten rooms, staff login with roles, the staff layout and its
  navigation. Reception asks for "is clean" before check-in and requests "mark dirty" after check-out; this feature
  does not check reservations or stays. A room that management adds later starts without a recorded cleaning status
  and is treated as Dirty (assumption H5) until a housekeeper marks it clean.

## Assumptions and Open Decisions
Decided here because the brief and steering leave them open; changing one requires revisiting this spec.

- **H1 Initial status**: all ten rooms are Clean after the first start, so the hotel can check guests in on go-live.
- **H2 Roles**: housekeepers and managers may open the cleaning list and mark rooms clean; receptionists may not
  (reception sees cleaning status in its own room overview).
- **H3 Order**: the cleaning list is ordered by room number, ascending.
- **H4 "Immediately"**: a change is visible on the cleaning list the next time it is opened or refreshed with its
  "Refresh" action; the open list is not pushed live.
- **H5 Missing status**: a room without a recorded cleaning status counts as Dirty: it is on the cleaning list and is
  not clean for check-in.
- **H6 One step**: "Mark clean" takes effect on one tap, without a confirmation question; there is no undo (a wrongly
  cleaned room is set dirty again only through reception's flows).
- **H7 Tablet** and desktop widths are those of foundation assumption A7 (768 px and 1280 px).

## Requirements

### Requirement 1: Cleaning status of rooms
**Objective:** As a housekeeper, I want every room to have a cleaning status of Dirty or Clean, so that everyone knows
which rooms can be given to the next guest.

#### Acceptance Criteria
1. The HRS shall give every room exactly one cleaning status out of Dirty and Clean.
2. The HRS shall show each of the ten rooms as Clean after its first start on an empty data store, without manual setup (assumption H1).
3. If a room has no recorded cleaning status, the HRS shall treat that room as Dirty (assumption H5).
4. If a cleaning status other than Dirty or Clean would be stored for a room, the HRS shall reject it and keep the room's existing cleaning status.
5. The HRS shall keep a room's cleaning status unchanged until the room is marked dirty or marked clean.

### Requirement 2: Cleaning list
**Objective:** As a housekeeper, I want to see all dirty rooms on my tablet, so that I know which rooms to clean next.

#### Acceptance Criteria
1. When a housekeeper opens the cleaning list, the HRS shall show every dirty room with its room number, ordered by room number ascending (assumption H3).
2. The HRS shall not show any clean room on the cleaning list.
3. If no room is dirty, the HRS shall show the message "All rooms are clean" instead of an empty list.
4. When a housekeeper opens the cleaning list again or chooses "Refresh", the HRS shall show the cleaning statuses as they are at that moment, including changes made by other staff members since the list was last shown (assumption H4).
5. When the cleaning list is shown at tablet width or desktop width (assumption H7), the HRS shall show every room number and its "Mark clean" action without horizontal scrolling.

### Requirement 3: Mark a room clean
**Objective:** As a housekeeper, I want to mark a room clean in one step after cleaning it, so that reception can give
it to the next guest.

#### Acceptance Criteria
1. When a housekeeper chooses "Mark clean" for a room on the cleaning list, the HRS shall set that room's cleaning status to Clean, remove the room from the cleaning list and show a confirmation naming the room number, without asking a further question (assumption H6).
2. If the room has already been marked clean by another staff member since the list was shown, the HRS shall keep the room Clean, remove it from the cleaning list and show the same confirmation.
3. If the room no longer exists when "Mark clean" is chosen, the HRS shall change no cleaning status, show a message that the room no longer exists, and show the refreshed cleaning list.

### Requirement 4: Access to the cleaning list
**Objective:** As the hotel, I want only housekeepers and managers to use the cleaning list, so that rooms are marked
clean only by the people responsible for cleaning.

#### Acceptance Criteria
1. The HRS shall let housekeepers and managers open the cleaning list and list it in their staff navigation menu (assumption H2).
2. If a receptionist opens the cleaning list, including by its address, the HRS shall refuse it in the same way as any staff page the role may not open, and shall not list it in the receptionist's navigation menu.
3. If a person who is not signed in opens the cleaning list, the HRS shall show the login page instead of the cleaning list.

### Requirement 5: Cleaning status for other features
**Objective:** As reception, I want to ask whether a room is clean and to mark a room dirty, so that check-in only uses
clean rooms and checked-out rooms reach the cleaning list at once.

#### Acceptance Criteria
1. When another feature marks a room dirty, the HRS shall set that room's cleaning status to Dirty, so that the room is on the cleaning list the next time the list is opened or refreshed, without any further step.
2. When another feature marks a room dirty that is already Dirty, the HRS shall keep it Dirty and show it exactly once on the cleaning list.
3. When another feature asks whether a room is clean, the HRS shall answer yes only if the room's cleaning status is Clean, and no if it is Dirty or not recorded.
4. When another feature asks for the cleaning status of all rooms, the HRS shall give the room number and cleaning status of every room, ordered by room number ascending.
5. If another feature asks about or marks a room that does not exist, the HRS shall report that the room is unknown and change no cleaning status.
