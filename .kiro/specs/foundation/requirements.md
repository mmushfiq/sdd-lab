# Requirements Document

## Project Description (Input)
Every other feature of the Hotel Reservation System needs rooms to exist and staff to be identified by role. Today
the app is an empty Spring Boot and Vaadin skeleton (`HrsApplication` with Lumo styles, test base classes
`HrsBrowserlessTest` and `PlaywrightIT`) with no schema, no login and no navigation. The foundation makes the
database hold the room types and the hotel's ten rooms (number, room type, capacity, nightly rate), lets staff
members (receptionist, manager, housekeeper) log in and land in a staff layout whose navigation shows only what their
role may open, keeps guest-facing pages public while refusing staff pages to anonymous users and to the wrong role,
and lets later specs add staff views and menu entries without touching foundation code beyond a menu entry. It also
provides one injected clock as the source of "today". Out of scope: editing rooms, room types or rates (management),
maintaining staff accounts through the UI (staff are seeded), any reservation or cleaning data. Constraints: passwords
hashed, no credentials in main reference data beyond what seeded staff need, staff layout usable on desktop and tablet.
Source: `.kiro/specs/foundation/brief.md`.

## Introduction
The foundation gives the Hotel Reservation System (HRS) its room reference data, staff sign-in with the three staff
roles, a shared staff layout with role-filtered navigation, a public entry page for guests, and one shared source of
the current date. Every later feature builds on these: they read rooms, restrict their pages to roles, and appear in
the staff navigation.

## Boundary Context
- **In scope**: room types and the ten rooms as reference data; staff members with one role each; staff login and
  logout; refusing staff pages to anonymous persons and to roles that may not open them; the staff layout, its
  navigation and a staff home page; the public entry page; one shared source of "today" and "now".
- **Out of scope**: changing rooms, room types, capacities or nightly rates (management); creating, editing or
  deleting staff members or changing passwords through the HRS (no page for it); cleaning status (housekeeping);
  reservations, availability and guest data (reservation-core and later specs); the guest search and booking pages
  themselves (guest-booking).
- **Adjacent expectations**: later features read rooms but do not create the ten rooms; they add their own staff
  pages, each declaring its permitted roles and a navigation title, and expect the foundation's layout and navigation
  to pick them up. The operator of an environment provides that environment's staff accounts and initial passwords.

## Assumptions and Open Decisions
These were left open by the brief and steering and are decided here; changing one requires revisiting this spec.

- **A1 Seed rooms** (placeholder values until management changes them):

  | Room type | Capacity | Nightly rate | Rooms |
  |-----------|----------|--------------|-------|
  | Single | 1 | 90.00 | 101, 102 |
  | Double | 2 | 130.00 | 103, 104, 105, 201, 202, 203 |
  | Family | 4 | 190.00 | 204, 205 |

- **A2 Staff accounts and passwords**: no staff password ships in the reference data delivered to every environment
  (steering `testing.md`: passwords only in test data). Each environment's staff accounts, with hashed initial
  passwords, are provided by its operator as environment-specific data; test and local development use the test data
  set. A password is changed the same way (operator supplies a new hash). A self-service "change password" page is
  not part of this spec.
- **A3 One role per staff member**; roles are exactly receptionist, manager, housekeeper.
- **A4 Landing page**: every role lands on the same staff home page after login.
- **A5 Hotel name** shown on pages: "Hotel Reservation System" (no real hotel name is given in the vision).
- **A6 Time zone**: "today" is the date in the time zone of the server the HRS runs on.
- **A7 Tablet** means a viewport width of 768 px; desktop means 1280 px or wider.

## Requirements

### Requirement 1: Room reference data
**Objective:** As a staff member, I want the hotel's room types and ten rooms to be present from the first start, so
that every later feature can work with real rooms.

#### Acceptance Criteria
1. The HRS shall hold exactly the ten rooms and three room types listed in assumption A1 after its first start on an empty data store, without manual setup.
2. The HRS shall give every room a unique room number and exactly one room type.
3. The HRS shall give every room type a unique name, a capacity of at least one guest, and a nightly rate greater than zero.
4. The HRS shall take a room's capacity and nightly rate from its room type.
5. If a room or room type would be stored with a duplicate room number or room type name, an unknown room type, a capacity below one, or a nightly rate of zero or less, the HRS shall reject it and keep the existing data unchanged.

### Requirement 2: Staff members and roles
**Objective:** As a manager, I want every staff member to have a personal login with exactly one role, so that the
system knows who may do what.

#### Acceptance Criteria
1. The HRS shall identify every staff member by a unique user name and give them a display name and exactly one role out of receptionist, manager and housekeeper.
2. If a staff member would be stored with a duplicate user name or a role other than receptionist, manager or housekeeper, the HRS shall reject it.
3. The HRS shall store staff passwords only in a one-way hashed form from which the password cannot be read back.
4. The HRS shall not contain any staff password in the reference data that is delivered to every environment.
5. The HRS shall not offer any page for creating, changing or deleting staff members or their passwords.

### Requirement 3: Login and logout
**Objective:** As a staff member, I want to log in with my user name and password and log out again, so that only I
act under my name.

#### Acceptance Criteria
1. When a staff member submits a correct user name and password on the login page, the HRS shall sign them in and show the staff home page inside the staff layout.
2. If the submitted user name is unknown or the password is wrong, the HRS shall keep the person signed out, stay on the login page and show one error message that does not reveal which of the two was wrong.
3. When a signed-in staff member chooses "Log out", the HRS shall end their session and show the public entry page.
4. When a person who has logged out opens a staff page again, the HRS shall require a new login before showing it.

### Requirement 4: Access to pages
**Objective:** As the hotel, I want guest pages to stay public and staff pages to open only for the permitted roles, so
that guest and staff functions cannot be mixed up or misused.

#### Acceptance Criteria
1. The HRS shall let anyone open the public entry page and the login page without signing in.
2. If a person who is not signed in opens a staff page, the HRS shall show the login page instead of the staff page.
3. If a signed-in staff member opens a staff page their role is not permitted to open, the HRS shall not show any content of that page and shall show a message that the page is not available.
4. The HRS shall decide access to every staff page by the roles permitted for that page, also when the page is opened by its address and not through the navigation.

### Requirement 5: Staff layout and navigation
**Objective:** As a staff member, I want one consistent staff layout whose menu shows only what my role may open, so
that I find my work quickly and never see pages I cannot use.

#### Acceptance Criteria
1. While a staff member is signed in, the HRS shall show every staff page inside one staff layout that shows the hotel name, the staff member's display name and role, the navigation menu and a "Log out" action.
2. The HRS shall list in the navigation menu exactly those staff pages the signed-in staff member's role may open.
3. The HRS shall provide a staff home page that receptionists, managers and housekeepers may open and that shows the signed-in staff member's display name, role and today's date.
4. Where a later feature adds a staff page with its permitted roles and a navigation title, the HRS shall list that page in the navigation menu for exactly those roles without any change to the staff layout.
5. When the staff layout is shown at tablet width or desktop width (assumption A7), the HRS shall keep the navigation menu and the "Log out" action reachable and show the page without horizontal scrolling.

### Requirement 6: Public entry page
**Objective:** As a guest, I want a public start page of the hotel, so that I can reach the guest functions without a
staff account.

#### Acceptance Criteria
1. When anyone opens the HRS start address, the HRS shall show the public entry page with the hotel name and a link to the staff login.
2. The public entry page shall not show the staff navigation menu or any staff or room-assignment data.

### Requirement 7: Current date and time
**Objective:** As the hotel, I want every feature to agree on what "today" is, so that date rules behave the same
everywhere and can be checked for a fixed day.

#### Acceptance Criteria
1. The HRS shall take "today" and "now" from one shared time source in the time zone given by assumption A6.
2. Where that time source is set to a fixed point in time, the HRS shall show and use that fixed date as today, including on the staff home page.
