# Proposal

## Why

Guests can only book by phone today. The vision's goal is that at least half of all reservations are made online
within six months, in under 3 minutes per booking, and that guests can view or cancel their own reservation without
calling the hotel.

## What Changes

- A public guest start page: the guest searches availability by dates and number of guests, picks a room, enters
  name, email and optional phone number, and books. No login.
- After booking, the guest sees a confirmation with the reservation number. Nothing is sent by email.
- The guest finds their reservation with the reservation number plus the email used for booking, and sees its
  details.
- The guest can cancel their reservation before the arrival date; on or after the arrival date they must contact
  the hotel.
- A wrong reservation number or email shows one generic "not found" message.
- The booking form tells the guest what their personal data is used for.

## Capabilities

### New Capabilities

- `online-booking`: guest self-service booking, finding a reservation by reservation number plus email, and guest
  cancellation.

### Modified Capabilities

None. Availability, the no-double-booking rule and the reservation number come unchanged from `availability` and
`reservations`.

## Impact

- New public (`@AnonymousAllowed`) guest views at route `""` and `my-reservation`.
- Reuses `AvailabilityService` and `ReservationService` from `add-reception-reservations`; adds a lookup by number
  and email and a guest cancellation rule.
- No database change.
