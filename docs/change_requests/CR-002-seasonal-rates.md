# CR-002: Seasonal Rates

**Requested by:** Hotel management
**Received:** 2026-10-27
**Status:** Specified, waiting for approval

## Request

> Right now every room type has just one nightly rate for the whole year. That doesn't work for us. In summer and
> around Christmas we're fully booked weeks ahead and could easily charge more. In November the rooms sit empty
> because our price is too high. At the moment I change the room type rate by hand before each season, and I
> sometimes forget to change it back.

## Parts

| Part | Change                                                                    | Kind             |
|------|---------------------------------------------------------------------------|------------------|
| a    | Manager defines seasons with their own nightly rate per room type         | New use case     |
| b    | Each night of a stay is priced at the nightly rate valid for that night   | Rule changed     |
| c    | The price of every night is stored and kept when the reservation changes  | New data         |
| d    | Early and overdue check-out price the nights from the stored night prices | Rule changed     |

## Decisions (stakeholder answers, 2026-10-27)

1. A season is a fixed date range (e.g. 1 Jul–31 Aug 2027), entered for each year; it does not repeat by itself →
   UC-025, Glossary Season.
2. A season sets its own nightly rate per room type; a room type without a season rate keeps its normal rate →
   UC-025 BR-003.
3. A stay that crosses a season boundary is priced night by night, each night at the rate valid for it →
   UC-001 BR-004.
4. At an early or overdue check-out, the reserved nights keep their price; additional nights get the rate valid for
   that night when they are added → UC-010 BR-002, BR-003.

## Assumptions (confirm on review)

- **Changing a Confirmed reservation (UC-005 BR-005) no longer recalculates every night.** Today a change prices the
  whole stay at the current rate. With seasons this would, for example, raise all nights of an early-booked July stay
  to the summer rate when the guest adds one night. New rule: nights that stay part of the reservation keep their
  price as long as the room type stays the same; added nights, and all nights after a change of room type, get the
  current rate. This follows decision 4.
- Seasons never overlap (UC-025 BR-002), so every night has exactly one rate. There are no priorities between seasons.
- A season is given by its first and last night, both included; a night belongs to the date it begins
  (Christmas season 20 Dec–2 Jan: guests leaving on 3 Jan pay the season rate for every night).
- Creating, changing or deleting a season never changes the price of a reservation already recorded (UC-025 BR-004),
  the same as a changed normal rate today (UC-016 BR-002).
- Seasons can be changed and deleted at any time, also in the past; this cannot change existing prices.
- Season rates can only be added for active room types (UC-025 BR-006).
- The search shows the nightly rate of each room type; when the nights have different rates, it shows the lowest and
  the highest. The summary before confirming (UC-001 step 5) lists the price of every night. Emails, the guest lookup
  (UC-002) and the reception screens keep showing the total price only.
- The room type's rate keeps its column `nightly_rate`; it is called the normal rate in the specifications.
- Existing reservations get their night prices from their total price: total ÷ number of nights for every night, any
  rounding remainder on the last night, so that their total prices stay exactly the same.
- No season is created automatically; until the manager creates one, every price stays as it is today.

## Impact

| Artifact                        | Affected | Change                                                                                        |
|---------------------------------|----------|-----------------------------------------------------------------------------------------------|
| requirements.md                 | yes      | New FR-027; FR-018 says normal rate; FR-012 and FR-018 back to Open                            |
| glossary.md                     | yes      | Nightly Rate, Room Type, Total Price changed; new Normal Rate, Season, Season Rate, Night Price |
| entity_model.md                 | yes      | New SEASON, SEASON_RATE, RESERVATION_NIGHT; ROOM_TYPE.nightly_rate is the normal rate; total price is the sum of the night prices |
| use_cases.puml                  | yes      | New UC-025 Maintain Seasonal Rates (Manager)                                                  |
| UC-025 Maintain Seasonal Rates  | new      | Approved                                                                                      |
| UC-001 Reserve Room Online      | yes      | Steps 3, 5, 9, A-flow recording, postcondition, BR-004 rewritten                              |
| UC-004 Create Reservation       | yes      | Step 3, postcondition, BR-004                                                                 |
| UC-005 Change Reservation       | yes      | Step 7, BR-005 rewritten                                                                      |
| UC-010 Check Out Guest          | yes      | BR-002, BR-003 rewritten; Implemented → Approved; NFR-006 Implemented → In Progress           |
| UC-016 Maintain Room Types      | yes      | "normal rate"; BR-002 rewritten; Implemented → Approved                                       |
| UC-002, UC-003, UC-006, UC-008  | no       | Show the total price only, which stays a stored value                                         |
| UC-020 View Revenue Report      | no       | Sums total prices, which stay correct                                                         |
| other use cases                 | no       | Searched for nightly rate and total price; no other reference                                 |

## Handoff to Construction

Run in this order once the spec change is approved:

1. `/flyway-migration`: new migration V008 that creates `season`, `season_rate` and `reservation_night`, and fills
   `reservation_night` for every existing reservation from its total price (see Assumptions). Never edit V001–V007.
   The test data (`src/test/resources/db/migration/V100`–`V120`) inserts reservations without nights: add a test
   migration between V120 and V199 (e.g. V198) that fills the missing nights the same way, so the test data files
   stay unchanged.
2. `/implement UC-025`, then `UC-016`, `UC-001`, `UC-004`, `UC-005`, `UC-010`. One shared pricing rule (UC-025 BR-003)
   used by search, reservation, change and check-out.
3. `/browserless-test` for the same use cases. Existing tests that compute prices as nights × rate stay valid when no
   season covers their dates; add tests for a stay crossing a season boundary, a room type without a season rate,
   overlapping seasons (UC-025 A3), UC-005 BR-005 (kept and added nights), and early and overdue check-out with
   different night prices (UC-010 BR-002, BR-003). Season test data must avoid the dates other tests search, or
   their expected prices change.
4. `/coverage-check` for the same use cases, then move the statuses forward.
