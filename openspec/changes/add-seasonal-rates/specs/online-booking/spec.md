## MODIFIED Requirements

### Requirement: Guest searches availability
The guest SHALL be able to search availability with arrival date, departure date and number of guests, with the
same rules and the same available rooms as the availability search for reception. Guests SHALL see each room's
room type, capacity and total price, but not its room number and no single nightly rate.

#### Scenario: Guest sees free rooms
- **WHEN** a guest searches from 10 March to 12 March for 2 guests
- **THEN** the system lists the free rooms that fit 2 guests with room type, capacity and total price
- **AND** does not show their room numbers

#### Scenario: Guest sees the seasonal price
- **WHEN** "Double" has the base rate 140.00 and the seasonal rate "Christmas" from 20 December 2030 at 180.00
- **AND** a guest searches from 18 December to 21 December 2030 for 2 guests
- **THEN** the system lists "Double" with the total price 460.00

#### Scenario: Guest search with invalid input
- **WHEN** a guest searches with an arrival date in the past
- **THEN** the system does not search
- **AND** shows the message "Arrival date cannot be in the past"

#### Scenario: No room available for the guest
- **WHEN** no room fits the guest's search
- **THEN** the system shows the message "No rooms available for these dates"

### Requirement: Guest finds their reservation
The guest SHALL find their reservation by entering the reservation number and the email used for booking.
Reservation number and email SHALL be matched ignoring upper and lower case and surrounding spaces. For a Booked
reservation whose online cancellation deadline has not passed, the system SHALL show the exact deadline.

#### Scenario: Reservation found
- **WHEN** a guest enters a reservation number and the matching email
- **THEN** the system shows the reservation's room type, dates, number of guests, guest name, price per night, total
  price and status

#### Scenario: Price per night shown to the guest
- **WHEN** a guest finds their reservation from 18 December to 21 December 2030 with the night rates 140.00, 140.00
  and 180.00
- **THEN** the system shows the price per night "2 nights × CHF 140.00, 1 night × CHF 180.00"
- **AND** the total price CHF 460.00

#### Scenario: Wrong reservation number or email
- **WHEN** a guest enters a reservation number that does not exist, or an email that does not match the reservation
- **THEN** the system shows the same message "No reservation found for this reservation number and email"
- **AND** shows no reservation data

#### Scenario: Cancelled reservation found
- **WHEN** a guest finds a reservation that is cancelled
- **THEN** the system shows it with the status Cancelled
- **AND** does not offer to cancel it

#### Scenario: Last cancellation time shown
- **WHEN** on 1 March 2030 a guest finds their Booked reservation whose online cancellation deadline is 8 March 2030,
  14:00
- **THEN** the system shows the text "Free online cancellation until 8 March 2030, 14:00 (Zurich time)"
