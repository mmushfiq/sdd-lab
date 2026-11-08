# Contract: Booking Confirmation Email

Sent once after a Guest books online (FR-006). Not sent for reservations created by a Receptionist, nor for
changes or cancellations.

| Part | Value |
|---|---|
| From | `hrs.mail.from` |
| To | the booking email as entered (trimmed) |
| Subject | `Your reservation <reservation number>` |
| Body | plain text, lines below |

Body:

```text
Thank you for your booking.

Reservation number: K7Q2M9XA
Room type: Double
Arrival: 12 March 2030
Departure: 14 March 2030
Guests: 2
Total price: 240.00

Free online cancellation until 10 March 2030, 15:00 (Zurich time)

View or cancel your reservation online with your reservation number and this email.
```

Failure: if the mail server cannot be reached or rejects the message, the booking stays saved, the guest
sees the normal confirmation, and a warning with the reservation number is logged. No retry.
