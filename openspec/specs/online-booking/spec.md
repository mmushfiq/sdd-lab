# online-booking Specification

## Purpose

Lets guests book a room online and view or cancel their own reservation without logging in and without calling the
hotel, identified only by reservation number plus email.

## Requirements

### Requirement: Guest pages without login
The guest booking page and the page to find a reservation SHALL be usable without logging in.

#### Scenario: Guest opens the booking page
- **WHEN** a visitor who is not logged in opens the hotel's start page
- **THEN** the system shows the availability search for guests without asking for a login

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

### Requirement: Guest books a room
After choosing a listed room, the guest SHALL enter name, email and an optional phone number and confirm the
booking. The system SHALL create a Booked reservation with the same rules as a reservation made by reception.

#### Scenario: Guest books successfully
- **WHEN** a guest chooses room "104" from 10 March to 12 March for 2 guests, enters "Anna Meier" and
  "anna@example.com" and confirms
- **THEN** the system saves a Booked reservation
- **AND** shows a confirmation with the reservation number, room type, dates, number of guests and total price
- **AND** tells the guest to keep the reservation number to view or cancel the reservation

#### Scenario: Guest data missing or invalid
- **WHEN** the guest confirms without a name, without an email or with an invalid email
- **THEN** the system does not save the reservation
- **AND** marks each invalid field with an error message

#### Scenario: Room taken while the guest was booking
- **WHEN** another reservation for the chosen room and overlapping nights is saved while the guest enters their data
- **AND** the guest confirms
- **THEN** the system does not save the reservation
- **AND** shows the message "The room is no longer available for these dates"
- **AND** returns the guest to the search results

### Requirement: Privacy notice
The booking form SHALL tell the guest that their name, email and phone number are used only to manage the
reservation.

#### Scenario: Privacy notice shown
- **WHEN** the guest sees the booking form
- **THEN** the form shows the text "We use your name, email and phone number only to manage your reservation."

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

### Requirement: Guest cancels their reservation
The guest SHALL be able to cancel their Booked reservation after confirming, only before its online cancellation
deadline.

#### Scenario: Guest cancels before the arrival date
- **WHEN** a guest cancels their Booked reservation before its online cancellation deadline and confirms
- **THEN** the reservation's status is Cancelled
- **AND** its room is available again for those nights

#### Scenario: Guest does not confirm
- **WHEN** a guest chooses to cancel but does not confirm
- **THEN** the reservation stays Booked

#### Scenario: Cancel on or after the arrival date
- **WHEN** a guest views their Booked reservation on or after its arrival date
- **THEN** the system does not offer to cancel it
- **AND** shows the message "The online cancellation deadline has passed. Please contact the hotel to cancel."

#### Scenario: Cancel after the deadline but before the arrival date
- **WHEN** on 9 March 2030 a guest views their Booked reservation for 10 March whose online cancellation deadline is
  8 March 2030, 14:00
- **THEN** the system does not offer to cancel it
- **AND** shows the message "The online cancellation deadline has passed. Please contact the hotel to cancel."

#### Scenario: Reservation from before the rule cancelled the day before arrival
- **WHEN** on 9 March 2030 at 20:00 a guest cancels their Booked reservation for 10 March whose online cancellation
  deadline is 10 March 2030, 00:00, and confirms
- **THEN** the reservation's status is Cancelled

### Requirement: Online cancellation deadline
Every reservation SHALL have an online cancellation deadline, in hotel time Europe/Zurich. Check-in is at 14:00. For
a reservation created after this rule goes live, the deadline SHALL be 48 hours before check-in: 14:00 two days before
the arrival date. A reservation that existed before SHALL keep its earlier deadline: 00:00 on the arrival date.

#### Scenario: New reservation
- **WHEN** a reservation for 10 March to 12 March 2030 is created after the rule goes live
- **THEN** its online cancellation deadline is 8 March 2030, 14:00

#### Scenario: Reservation from before the rule
- **WHEN** a reservation for 10 March to 12 March 2030 was created before the rule went live
- **THEN** its online cancellation deadline is 10 March 2030, 00:00

### Requirement: Booking confirmation email
After a guest books a room online, the system SHALL send a confirmation email to the guest's email. The email SHALL
contain the reservation number, room type, arrival and departure date, number of guests, total price and the online
cancellation deadline. If the email cannot be sent, the booking SHALL still be saved and confirmed to the guest.

#### Scenario: Email sent after booking
- **WHEN** guest "Anna Meier" books a Double room online from 10 March to 12 March 2030 for 2 guests with the email
  "anna@example.com"
- **THEN** the system sends an email to "anna@example.com" with the reservation number, room type "Double", the dates
  10 March 2030 to 12 March 2030, 2 guests, the total price
- **AND** the text "Free online cancellation until 8 March 2030, 14:00 (Zurich time)"

#### Scenario: Email cannot be sent
- **WHEN** a guest books a room online and the email cannot be sent
- **THEN** the system saves the Booked reservation
- **AND** shows the booking confirmation as usual

#### Scenario: Reservation made by reception
- **WHEN** the receptionist creates a reservation
- **THEN** the system sends no confirmation email
