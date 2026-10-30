# Domain Language

The words HRS uses for its business, so that specs, code, UI texts and tests all say the same thing. Source:
`docs/vision.md`. Product scope and invariants are in `product.md`; this file only fixes the vocabulary.

## Rules
- Use the **preferred term** in requirements, designs, class names, UI labels and test names.
- Do not use the **avoid** words, even in comments; they either mean something else or are ambiguous.
- Code uses the English term in its singular form (`Reservation`, `RoomType`, `CleaningStatus`); UI labels use the
  same term in plain words ("Room type").
- A new business term is added here before it appears in a spec.

## Roles

| Term | Meaning | Avoid |
|------|---------|-------|
| **Guest** | A person who reserves and stays in a room. Uses the public pages without a staff account. | customer, client, user |
| **Receptionist** | Staff member at the front desk: reservations for walk-in and phone guests, check-in, check-out. | front desk, clerk |
| **Manager** | Staff member who maintains rooms, room types and rates and reads reports. | admin, owner |
| **Housekeeper** | Staff member who cleans rooms and marks them clean. The work area is called *housekeeping*. | cleaner, maid, room service |
| **Staff member** | Any person with a login: receptionist, manager or housekeeper. | employee, user, account |

"User" is reserved for technical contexts (Spring Security); in business text say guest or staff member.

## Rooms

| Term | Meaning | Avoid |
|------|---------|-------|
| **Room** | One of the hotel's ten bookable rooms, identified by its **room number**. | unit, space |
| **Room type** | A category of rooms that share capacity and nightly rate (for example single, double). | category, class |
| **Capacity** | The maximum number of guests a room type sleeps. | size, beds |
| **Nightly rate** | The price for one night in a room of a room type. | price per day, tariff |
| **Cleaning status** | Whether a room is **Dirty** or **Clean**. Only these two values. | ready, available (for cleanliness) |
| **Cleaning list** | The housekeeper's list of rooms that are dirty. | task list, to-do |

## Reservations and Stays

| Term | Meaning | Avoid |
|------|---------|-------|
| **Reservation** | A guest's claim on one room for a range of nights. Created online by the guest or by a receptionist. | booking (as a noun), order |
| **Reserve / book** | Creating a reservation. "Book" is fine as a verb in guest-facing text. | |
| **Arrival date** | The first night of a reservation. | start date, check-in date |
| **Departure date** | The day the guest leaves; that night is *not* part of the reservation. | end date, check-out date |
| **Night** | One overnight stay between two dates; a reservation from 1 to 3 June has two nights. | day |
| **Availability** | Rooms that are free for every night of a date range and fit the number of guests. | vacancy, free rooms |
| **Walk-in guest** | A guest without a reservation who arrives at the desk; the receptionist creates the reservation. | |
| **Check-in** | The receptionist records that the guest has arrived and occupies the room. Only allowed for a clean room. | arrival (as an action) |
| **Check-out** | The receptionist records that the guest has left; the room becomes dirty and appears on the cleaning list. | departure (as an action) |
| **Stay** | The time between check-in and check-out. | visit |
| **Cancel** | Ending a reservation before check-in, by the guest online or by a receptionist. The reservation is kept, marked cancelled, not deleted. | delete, remove |
| **Double booking** | Two reservations for the same room with at least one common night. Must never happen. | overbooking |

### Reservation lifecycle

```
Reserved ──check-in──▶ Checked in ──check-out──▶ Checked out
   │
   └──cancel──▶ Cancelled
```

Statuses (reservation-core): `RESERVED`, `CHECKED_IN`, `CHECKED_OUT`, `CANCELLED`. Allowed changes are exactly the
arrows above; there is no no-show status.

## Reports

| Term | Meaning | Avoid |
|------|---------|-------|
| **Occupancy** | Share of available room nights that were occupied in a period, shown in percent. | utilisation, load |
| **Revenue** | The booked nights of stays in a period, each at the reservation's booked nightly rate. Payment itself is out of scope. | income, sales, turnover |

## Decisions

Answers the specs have fixed; the spec named in brackets owns each one.

- **Reservation code** [reservation-core]: 8 random letters and digits, unique per reservation.
- **Price** [reservation-core]: the nightly rate is fixed at booking time; price = nights × booked rate. Moving a
  reservation to another room type takes that type's current rate.
- **Guest data** [reservation-core]: name plus at least one of e-mail or phone; stored per reservation, no guest
  profiles. Erased 90 days after departure (not while checked in); the reservation itself is kept for reports.
- **Changes** [reservation-core]: only while Reserved, and only room, dates and number of guests. One room per
  reservation; arrival dates in the past are refused.
- **Initial cleaning status** [housekeeping]: all rooms start Clean; a room without a recorded status counts as Dirty.
- **Cleaning list access** [housekeeping]: housekeepers and managers; receptionists are refused.
- **Staff accounts** [foundation]: one role per staff member; staff are provisioned per environment, not through the
  UI.
- **Guest access** [guest-booking]: a guest opens their reservation with reservation code plus the e-mail stored with
  it; online booking needs name and e-mail. Online cancel while Reserved until the day before arrival; no online
  change; no confirmation e-mail.
- **Reservation handling by staff** [front-desk-reservations]: receptionists only; online and desk reservations are
  treated the same; guest data is not edited (cancel and re-create).
- **No-show** [reception]: no own status. A Reserved reservation past its arrival date shows as **Late arrival**;
  reception or the guest may cancel it, otherwise it stays Reserved and never counts as a stay.
- **Check-in and check-out window** [reception]: check-in from the arrival date to the day before departure; check-out
  any time while checked in. A checked-in guest past the departure date shows as **Overdue** and keeps the room.
  No room move at check-in. Reception pages are for receptionists only.
- **Stay record** [reception]: the actual check-in day and check-out day of a reservation. A reservation is a stay when
  its status is Checked in or Checked out.
- **Rooms out of service** [management]: not supported. A room can be removed only if it never had a reservation; a
  room type only if no room uses it. Rate changes apply only to reservations created or moved afterwards.

- **Report definitions** [reports]: an *occupied night* is a night actually stayed by a Checked in or Checked out
  reservation (check-in day to the night before check-out; without a stay record, arrival to departure); Reserved
  and Cancelled never count. *Room nights available* = current number of rooms × nights of the period. *Revenue*
  counts each booked night of a stay at its booked rate in the month of that night; Overdue nights earn nothing.
  Occupancy is rounded half up to one decimal; totals are computed from summed nights and amounts.

## Open Questions

The vision does not settle these; the spec that needs one must decide it and record the answer here. None are open
at the moment.

---
_Vocabulary only. Rules and flows belong in the specs' requirements._
