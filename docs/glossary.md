# Glossary

| Term               | Definition                                                                                                                                      | Avoid                       |
|--------------------|-------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------|
| Guest              | A person who reserves a room or stays at the hotel. A guest needs no login; they reach their reservation with the reservation code.            | Customer, Client            |
| Staff Member       | An employee of the hotel who logs in to the system with a username, a password and exactly one role (receptionist, manager or housekeeper).    | User                        |
| Receptionist       | A staff member at the front desk who manages reservations, check-ins and check-outs on site.                                                   | Clerk, Front Desk Agent     |
| Manager            | A staff member who maintains rooms, room types and staff members and views the reports.                                                        | Admin, Owner                |
| Housekeeper        | A staff member who cleans rooms and records their cleaning status.                                                                             | Cleaner, Maid               |
| Room               | One physical hotel room with a unique room number. It belongs to exactly one room type.                                                        | Unit                        |
| Room Type          | A class of rooms with the same name, description, maximum number of guests and normal rate, for example "Double Room". Guests reserve a room type, not a room. | Category, Room Class |
| Nightly Rate       | The price of one night in a room of a given room type on a given date: the season rate of the room type when a season covers that date, otherwise the normal rate. | Price per Night, Tariff     |
| Normal Rate        | The nightly rate of a room type on every date for which no season sets a season rate for it.                                                   | Base Rate, Standard Rate    |
| Season             | A named date range, from its first night to its last night, for which the manager sets season rates, for example "Summer 2027". Seasons never overlap. | Rate Period, Tariff Period  |
| Season Rate        | The nightly rate of one room type for the nights of one season. A season can leave a room type without a season rate; the normal rate applies then. | Seasonal Price, Surcharge   |
| Availability       | The number of rooms of a room type that are neither reserved, occupied nor blocked for every night of a date range.                           | Vacancy, Free Capacity      |
| Reservation        | A guest's claim on one room type for a date range, made online or by a receptionist. It has the status confirmed, checked in, checked out, cancelled or no-show. | Booking |
| Reservation Code   | The unique random code a guest receives on confirmation and uses together with their email address to view or cancel their reservation.        | Booking Number, PIN         |
| Arrival Date       | The first date of a reservation, on which the guest is expected to check in.                                                                   | Start Date                  |
| Departure Date     | The date on which the guest checks out; the night before it is the last night of the reservation.                                              | End Date                    |
| Night              | One overnight period of a reservation. A reservation from arrival date to departure date has (departure date − arrival date) nights.          |                             |
| Night Price        | The price of one night of a reservation: the nightly rate valid for that night, fixed when the night is recorded.                              |                             |
| Total Price        | The sum of the night prices of all nights of the reservation.                                                                                  | Amount                      |
| Check-in           | The event at which the receptionist assigns a clean room to a guest with a reservation and the guest starts their stay.                       |                             |
| Check-out          | The event at which the guest ends their stay, the room is released and its cleaning status becomes dirty.                                      |                             |
| Check-in Time      | The time of day from which a guest can check in on the arrival date (14:00).                                                                   |                             |
| Check-out Time     | The time of day by which a guest must check out on the departure date (11:00).                                                                 |                             |
| Stay               | The period during which a checked-in guest occupies a room, from check-in to check-out.                                                       | Visit                       |
| Cancellation       | Ending a reservation before check-in. A guest can cancel online until the online cancellation deadline of the reservation; a receptionist can cancel at any time before check-in. | Storno |
| Online Cancellation Deadline | The moment from which a guest can no longer cancel a reservation online. It is fixed when the reservation is recorded: 48 hours before the check-in time on the arrival date. Reservations recorded before this rule took effect keep 24 hours until their arrival date is changed. | Cancellation Period, Cutoff, Storno Deadline |
| No-Show            | A reservation whose guest did not check in by the end of the arrival date.                                                                     |                             |
| Cleaning Status    | The state of a room regarding cleaning: clean or dirty.                                                                                         | Housekeeping Status         |
| Clean              | Cleaning status of a room that is ready to be assigned to a guest.                                                                             |                             |
| Dirty              | Cleaning status of a room that must be cleaned before it can be assigned; set at check-out and, for occupied rooms, at the start of each day.   | Unclean                     |
| Cleaning List      | The list of dirty rooms that housekeepers work through, with rooms of today's departures and check-outs first.                               | Task List                   |
| Room Block         | A date range during which a room is out of service (for example for repairs) and can neither be reserved nor assigned.                        | Out of Order, Maintenance   |
| Room Status Board  | The screen showing every room with its occupancy (free, occupied, blocked) and its cleaning status.                                           | Dashboard                   |
| Occupancy Rate     | The number of occupied room nights divided by the number of available room nights (rooms not blocked) in a period, in percent.               | Utilisation, Load           |
| Revenue            | The sum of the total prices of all checked-out reservations in a period.                                                                       | Income, Turnover            |
| Erasure            | Removing a guest's personal data (name, email address, phone number) on the guest's request, or replacing it with values that no longer identify the guest. Reservations of the guest are kept without identifying them, so reports stay correct. |                             |
