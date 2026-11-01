## ADDED Requirements

### Requirement: Changed reservation keeps its deadline rule
When the receptionist changes the arrival date of a reservation, the reservation SHALL keep the online cancellation
deadline rule it was created with, and its deadline SHALL be calculated again from the new arrival date.

#### Scenario: New-rule reservation moved
- **WHEN** the receptionist changes a reservation created under the 48-hour rule from arrival 10 March 2030 to arrival
  20 March 2030
- **THEN** its online cancellation deadline is 18 March 2030, 14:00

#### Scenario: Reservation from before the rule moved
- **WHEN** the receptionist changes a reservation created before the 48-hour rule from arrival 10 March 2030 to
  arrival 20 March 2030
- **THEN** its online cancellation deadline is 20 March 2030, 00:00
