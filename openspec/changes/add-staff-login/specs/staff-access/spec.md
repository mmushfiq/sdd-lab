# Spec Delta

## Purpose

Controls who can use the staff part of the Hotel Reservation System: staff log in with one role (Receptionist,
Manager or Housekeeping), each staff page is open only to its roles, and guest pages stay public.

## ADDED Requirements

### Requirement: Staff roles
Every staff user SHALL have exactly one role: Receptionist, Manager or Housekeeping.

#### Scenario: Staff user has one role
- **WHEN** a staff user is logged in
- **THEN** the system grants that user the permissions of exactly one role: Receptionist, Manager or Housekeeping

### Requirement: Provided staff users
The system SHALL provide at least one staff user for each role when it is installed. The system SHALL NOT offer a
page to create, change or delete staff users.

#### Scenario: One staff user per role after installation
- **WHEN** the system has just been installed
- **THEN** a Receptionist, a Manager and a Housekeeping staff user can each log in

### Requirement: Staff login
The system SHALL let a staff user log in with username and password and SHALL show the staff start page after a
successful login.

#### Scenario: Successful login
- **WHEN** a staff user enters a valid username and the matching password
- **THEN** the system logs the user in
- **AND** shows the staff start page

#### Scenario: Wrong password
- **WHEN** a staff user enters a valid username and a wrong password
- **THEN** the system does not log the user in
- **AND** shows the message "Invalid username or password"

#### Scenario: Unknown username
- **WHEN** someone enters a username that does not belong to any staff user
- **THEN** the system does not log anyone in
- **AND** shows the same message "Invalid username or password"

#### Scenario: Empty username or password
- **WHEN** someone submits the login form with an empty username or an empty password
- **THEN** the system does not log anyone in

### Requirement: Staff start page
The staff start page SHALL show the name and the role of the logged-in staff user and SHALL offer a logout action.

#### Scenario: Start page shows the logged-in user
- **WHEN** a Receptionist is logged in and opens the staff start page
- **THEN** the page shows the user's name and the role "Receptionist"
- **AND** offers a logout action

### Requirement: Staff logout
The system SHALL let a logged-in staff user log out. After logout, staff pages SHALL require a new login.

#### Scenario: Logout
- **WHEN** a logged-in staff user logs out
- **THEN** the system ends the user's session
- **AND** shows the login page

#### Scenario: Staff page after logout
- **WHEN** a staff user has logged out and opens a staff page again
- **THEN** the system shows the login page instead of the staff page

### Requirement: Staff pages require login
A visitor who is not logged in SHALL NOT see any staff page; the system SHALL show the login page instead.

#### Scenario: Anonymous visitor opens a staff page
- **WHEN** a visitor who is not logged in opens a staff page
- **THEN** the system shows the login page instead of the staff page

### Requirement: Staff pages are restricted by role
Each staff page SHALL be open only to the roles it is meant for. A logged-in staff user whose role is not allowed
SHALL NOT see the page or its data.

#### Scenario: Role is allowed
- **WHEN** a logged-in staff user opens a staff page meant for their role
- **THEN** the system shows the page

#### Scenario: Role is not allowed
- **WHEN** a logged-in Housekeeping user opens a staff page meant only for the Manager
- **THEN** the system does not show the page or any of its data
- **AND** tells the user that access is denied

### Requirement: Guest pages are public
Pages meant for guests SHALL be usable without logging in.

#### Scenario: Guest opens a guest page
- **WHEN** a visitor who is not logged in opens a page meant for guests
- **THEN** the system shows the page without asking for a login

### Requirement: Passwords are not stored readable
The system SHALL store staff passwords only in a form from which the password cannot be read back.

#### Scenario: Stored password is not readable
- **WHEN** the stored data of a staff user is inspected
- **THEN** it does not contain the user's password in readable form
