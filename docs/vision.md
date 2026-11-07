# Vision: Hotel Reservation System (HRS)

## Mission

We own a small hotel with ten rooms. Reservations, check-in/check-out and room cleaning are handled today with
paper lists and phone calls, which leads to double bookings, lost reservations and guests waiting for rooms that are
not yet clean. The Hotel Reservation System gives guests a simple way to book online and gives our staff one shared,
up-to-date view of every room: who has booked it, who is staying in it, and whether it is clean.

## Target users

- **Guest (online booking):** searches for available rooms for a date range, books a room, and views or cancels
  their own reservation without calling the hotel.
- **Receptionist (on-site management):** creates and changes reservations for walk-in and phone guests, checks guests
  in and out, and sees the current state of all rooms at a glance.
- **Manager (reporting and administration):** maintains rooms, room types and rates, and views occupancy and revenue
  reports to run the business.
- **Housekeeping (room service):** sees which rooms need cleaning and marks rooms as cleaned so they can be given to
  the next guest.

## Goals

- No double bookings: a room can never be reserved by two guests for overlapping nights.
- A guest can only be checked in to a room that is clean.
- A guest can complete an online booking in under 3 minutes.
- Reception can check a guest in or out in under 1 minute.
- After check-out, housekeeping sees the room in its cleaning list immediately.

## Scope

### In scope

- Room and room type management (number, type, capacity, nightly rate)
- Availability search by date range and number of guests
- Online booking, viewing and cancelling of reservations by guests
- Reservation management by reception (create, change, cancel)
- Guest check-in and check-out
- Room cleaning status (dirty, clean) and housekeeping cleaning list
- Occupancy and revenue reports for the manager
- Login and role-based access for receptionist, manager and housekeeping

### Out of scope

- Online payment processing (payment is settled at the hotel)
- Integration with booking channels (Booking.com, Expedia, etc.)
- Restaurant, minibar and other extra services billing
- Multiple hotels / multi-property support
- Loyalty programs

## Constraints

- The hotel has exactly ten rooms; the system does not need to scale beyond a single small property.
- Technology stack: Java 25, Spring Boot 4, Vaadin 25, jOOQ, Flyway, H2.
- Guest personal data must be handled in line with GDPR.
- The staff UI must be usable on a desktop at the reception desk and on a tablet for housekeeping.

## Success measures

- Zero double bookings after go-live.
- At least half of all reservations are made online by guests within six months.
- No guest is checked in to a room that is not marked clean.
- The manager can see monthly occupancy and revenue without manual spreadsheet work.
