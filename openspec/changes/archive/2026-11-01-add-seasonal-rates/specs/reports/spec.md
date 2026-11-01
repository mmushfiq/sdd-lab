## MODIFIED Requirements

### Requirement: Monthly revenue
The revenue of a month SHALL be the sum of the saved rates of its occupied room-nights, each night with its own saved
rate, shown in CHF with two decimals. Later changes of base rates or seasonal rates SHALL NOT change past revenue.

#### Scenario: Revenue from saved rates
- **WHEN** April has a Checked out stay of 3 nights at the rate 140.00 and one of 2 nights at 220.00
- **THEN** the report shows revenue 860.00 for April

#### Scenario: Stay with different rates across two months
- **WHEN** a Checked out stay has the night of 31 December 2030 at 180.00 and the nights of 1 and 2 January 2031 at
  180.00 and 140.00
- **THEN** the report for 2030 shows revenue 180.00 for December
- **AND** the report for 2031 shows revenue 320.00 for January

#### Scenario: Rate changed after the stay
- **WHEN** the manager changes the base rate or a seasonal rate of the room type after a stay in April
- **THEN** the revenue for April does not change
