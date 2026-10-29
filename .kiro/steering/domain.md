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

The vision names these steps; the exact status names and any further states are fixed in the spec that introduces
reservations.

## Reports

| Term | Meaning | Avoid |
|------|---------|-------|
| **Occupancy** | Share of available room nights that were occupied in a period, shown in percent. | utilisation, load |
| **Revenue** | Money earned from nights stayed in a period, at the nightly rates of those reservations. Payment itself is out of scope. | income, sales, turnover |

## Open Questions

The vision does not settle these; the spec that needs one must decide it and record the answer here:

- How a guest finds their own reservation online without a staff account (for example reservation code plus e-mail).
- Whether and until when a guest may cancel online, and whether a guest may change a reservation online.
- What happens to a reservation whose guest never arrives (no-show).
- Whether rooms or room types can be taken out of service, and what that does to existing reservations.
- Exact definitions for the reports: which nights count as occupied, which reservations count as revenue (checked
  out only, or also checked in).
- Which guest personal data a reservation needs (GDPR: as little as possible) and how long it is kept.

---
_Vocabulary only. Rules and flows belong in the specs' requirements._
