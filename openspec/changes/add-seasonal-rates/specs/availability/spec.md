## MODIFIED Requirements

### Requirement: Search result details
Each listed room SHALL show its room number, room type, capacity and the total price for the stay. The total price
SHALL be the sum of the rates of its nights for the room's room type, each night priced on its own.

#### Scenario: Total price shown
- **WHEN** room "104" (Double, base rate 140.00, no seasonal rate for these nights) is listed for a stay of 2 nights
- **THEN** the result shows the total price 280.00

#### Scenario: Total price across the start of a season
- **WHEN** "Double" has the base rate 140.00 and the seasonal rate "Christmas" from 20 December 2030 at 180.00
- **AND** room "104" (Double) is listed for a stay from 18 December to 21 December 2030
- **THEN** the result shows the total price 460.00

#### Scenario: No single nightly rate shown
- **WHEN** rooms are listed for a stay
- **THEN** the result shows no nightly rate column
