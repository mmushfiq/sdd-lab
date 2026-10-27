# Requirements Catalog: Hotel Reservation System (HRS)

Source: [vision.md](vision.md) · Terms: [glossary.md](glossary.md)

## Functional Requirements

### Guest (online)

| ID     | Title                           | User Story                                                                                                                                                                              | Priority | Status |
|--------|---------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|--------|
| FR-001 | Search Availability             | As a guest, I want to search the available room types for an arrival date, a departure date and a number of guests so that I can see which rooms I can reserve and what they cost.      | High     | Open   |
| FR-002 | Reserve Room Type Online        | As a guest, I want to reserve an available room type for my dates by entering my name, email address and phone number so that I have a guaranteed room without calling the hotel.       | High     | Open   |
| FR-003 | Receive Reservation Confirmation | As a guest, I want to receive a confirmation email with my reservation code and my online cancellation deadline after reserving so that I have proof of my reservation and can find it again later.                        | High     | Open   |
| FR-004 | View Own Reservation            | As a guest, I want to look up my reservation with my reservation code and email address so that I can check my dates, room type, total price and until when I can cancel online.                                        | Medium   | Open   |
| FR-005 | Cancel Own Reservation          | As a guest, I want to cancel my reservation online until the online cancellation deadline of my reservation so that I do not have to call the hotel when my plans change.             | High     | Open   |

### Receptionist

| ID     | Title                       | User Story                                                                                                                                                                                     | Priority | Status |
|--------|-----------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|--------|
| FR-006 | Create Reservation          | As a receptionist, I want to create a reservation for a guest who calls or walks in so that phone and walk-in guests are recorded in the same system as online reservations.                    | High     | Open   |
| FR-007 | Change Reservation          | As a receptionist, I want to change the dates, room type or number of guests of a reservation so that I can handle a guest's change request without cancelling and re-creating it.             | High     | Open   |
| FR-008 | Cancel Reservation          | As a receptionist, I want to cancel any reservation before check-in so that the room type becomes available again for other guests.                                                          | High     | Open   |
| FR-009 | Search Reservations         | As a receptionist, I want to search reservations by guest name, reservation code or date so that I can answer a guest's question about their reservation while they wait.                                  | High     | Implemented |
| FR-010 | View Arrivals and Departures | As a receptionist, I want to see the list of today's arrivals and departures so that I can prepare check-ins and check-outs.                                                                    | High     | Implemented |
| FR-011 | Check In Guest              | As a receptionist, I want to check in a guest by assigning a clean, free room of the reserved room type so that the guest gets a room that is ready for them.                                 | High     | Implemented |
| FR-012 | Check Out Guest             | As a receptionist, I want to check out a guest and see the total price of the stay so that the room is released and its cleaning status is set to dirty for housekeeping.                     | High     | Open   |
| FR-013 | View Room Status Board      | As a receptionist, I want to see every room with its occupancy and cleaning status on one screen so that I know at a glance which rooms I can assign.                                         | High     | Implemented |
| FR-014 | Record No-Show              | As a receptionist, I want to mark a reservation as a no-show when the guest has not checked in by the end of the arrival date so that the room type becomes available again.                   | Medium   | Implemented |

### Housekeeper

| ID     | Title                   | User Story                                                                                                                                                                  | Priority | Status |
|--------|-------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|--------|
| FR-015 | View Cleaning List      | As a housekeeper, I want to see the cleaning list with all dirty rooms, departures first, so that I know which rooms to clean and in which order.                          | High     | Implemented |
| FR-016 | Mark Room Clean         | As a housekeeper, I want to mark a room as clean after cleaning it so that reception can assign it to the next guest.                                                       | High     | Implemented |
| FR-017 | Daily Cleaning of Occupied Rooms | As a housekeeper, I want every occupied room to be set to dirty at the start of each day so that staying guests get their room cleaned daily.                       | Medium   | Verified |

### Manager

| ID     | Title                  | User Story                                                                                                                                                             | Priority | Status |
|--------|------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|--------|
| FR-018 | Maintain Room Types    | As a manager, I want to create and edit room types with name, description, maximum number of guests and normal rate so that guests see correct offers and prices.      | High     | Open   |
| FR-019 | Maintain Rooms         | As a manager, I want to create and edit rooms with room number and room type so that availability is calculated from the rooms the hotel really has.                   | High     | Implemented |
| FR-020 | Block Room             | As a manager, I want to block a room for a date range so that a room under repair is neither reserved nor assigned.                                                    | Medium   | Implemented |
| FR-021 | View Occupancy Report  | As a manager, I want to see the occupancy rate per day and per month for a chosen period so that I can understand how well the hotel is used.                          | High     | Implemented |
| FR-022 | View Revenue Report    | As a manager, I want to see the revenue per month and per room type for a chosen period so that I can run the business without manual spreadsheet work.                | High     | Implemented |
| FR-023 | Maintain Staff Members | As a manager, I want to create, edit and deactivate staff members with their role so that only current employees can use the staff functions.                         | High     | Implemented |
| FR-026 | Erase Guest Data       | As a manager, I want to erase or anonymise a guest's personal data on the guest's request so that the hotel meets its obligation under the right to erasure within 30 days. | High     | Implemented |
| FR-027 | Maintain Seasonal Rates | As a manager, I want to define seasons as date ranges with their own nightly rate per room type so that prices rise in busy periods and fall in quiet ones without changing the normal rate by hand before and after each season. | High     | Verified |

### Staff member

| ID     | Title          | User Story                                                                                                                                      | Priority | Status |
|--------|----------------|-------------------------------------------------------------------------------------------------------------------------------------------------|----------|--------|
| FR-024 | Staff Login    | As a staff member, I want to log in with my username and password so that I can use the functions of my role and guests' data stays protected. | High     | Open   |
| FR-025 | Staff Logout   | As a staff member, I want to log out so that nobody else can use my session at a shared reception desk or tablet.                              | Medium   | Implemented |

## Non-Functional Requirements

| ID      | Title                         | Requirement                                                                                                                                       | Category        | Priority | Status |
|---------|-------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------|-----------------|----------|--------|
| NFR-001 | Availability Search Time      | 95% of availability searches return results within 1 second.                                                                                      | Performance     | High     | Open   |
| NFR-002 | Page Display Time             | 95% of pages are fully displayed within 2 seconds.                                                                                                     | Performance     | High     | In Progress |
| NFR-003 | Confirmation Email Delay      | The reservation confirmation email is sent within 1 minute after the reservation is created.                                                     | Performance     | Medium   | Open   |
| NFR-004 | No Concurrent Overbooking     | When 10 reservation requests for the last available room of a room type arrive at the same time, exactly one succeeds and the others are refused. | Scalability     | High     | In Progress |
| NFR-005 | Online Reservation Duration   | A first-time guest completes an online reservation in under 3 minutes in a usability test with 5 participants.                                   | Usability       | High     | Open   |
| NFR-006 | Check-in and Check-out Duration | A trained receptionist completes a check-in or a check-out in under 1 minute.                                                                   | Usability       | High     | In Progress |
| NFR-007 | Tablet Layout                 | Cleaning list and room status screens are fully usable without horizontal scrolling on a tablet with a screen width of 768 px.                   | Usability       | High     | Implemented |
| NFR-008 | Guest Accessibility           | The guest screens (search, reserve, view, cancel) conform to WCAG 2.1 level AA.                                                                  | Usability       | Medium   | Open   |
| NFR-009 | Role-Based Access             | Every staff screen and action is only available to the roles that need it; a guest without login can only reach the guest screens.              | Security        | High     | In Progress |
| NFR-010 | Password Storage              | Staff passwords are stored only as salted hashes with bcrypt (cost factor 10 or higher) and are never shown or logged.                           | Security        | High     | In Progress |
| NFR-011 | Reservation Code Strength     | A reservation code has at least 8 random characters, and a reservation lookup is blocked for 15 minutes after 5 failed attempts from one IP address.  | Security        | High     | Open   |
| NFR-012 | Encrypted Transport           | All traffic between browser and server uses HTTPS with TLS 1.2 or higher.                                                                        | Security        | High     | Open   |
| NFR-013 | Session Timeout               | A staff session ends automatically after 30 minutes without activity.                                                                            | Security        | Medium   | In Progress |
| NFR-014 | System Uptime                 | The system is available 99% of the time per calendar month.                                                                                       | Availability    | Medium   | Open   |
| NFR-015 | Data Backup                   | The database is backed up once per day and a backup can be restored within 1 hour.                                                               | Availability    | Medium   | Open   |

## Constraints

| ID    | Title                  | Constraint                                                                                                                  | Category   | Source             | Priority | Status |
|-------|------------------------|-----------------------------------------------------------------------------------------------------------------------------|------------|--------------------|----------|--------|
| C-001 | Runtime Platform       | The backend runs on Java 25 with Spring Boot 4.                                                                             | Technical  |                    | High     | Open   |
| C-002 | UI Framework           | The UI is built with Vaadin 25 (Flow, server-side Java).                                                        | Technical  |                    | High     | Open   |
| C-003 | Data Access            | Data access uses jOOQ, and the database schema is managed with Flyway migrations.                                           | Technical  |                    | High     | Open   |
| C-004 | Database               | The system uses an H2 database.                                                                                             | Technical  |                    | High     | Open   |
| C-005 | Browser Support        | The UI supports the latest 2 versions of Chrome, Firefox, Safari and Edge on desktop and tablet.                           | Technical  |                    | High     | Open   |
| C-006 | Single Small Property  | The system serves one hotel with ten rooms; multiple properties are not supported.                                          | Business   |                    | High     | Implemented |
| C-007 | No Online Payment      | The system does not process payments; the guest pays at the hotel.                                                         | Business   |                    | High     | In Progress |
| C-008 | Data Minimisation      | Only the guest data needed for the reservation and stay (name, email address, phone number) is collected.                  | Regulatory | GDPR Art. 5(1)(c)  | High     | In Progress |
| C-009 | Information to Guests  | The guest is shown a privacy notice before submitting an online reservation.                                               | Regulatory | GDPR Art. 13       | High     | Open   |
| C-010 | Right to Erasure       | Personal data of a guest is erased or anonymised within 30 days of the guest's request, unless a legal retention duty applies. | Regulatory | GDPR Art. 17(1), 12(3) | High | Implemented |
