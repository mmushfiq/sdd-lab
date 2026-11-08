# Quickstart: Hotel Reservation System

How to run and check the feature end to end. Routes: [contracts/ui-routes.md](contracts/ui-routes.md).
Data: [data-model.md](data-model.md).

## Prerequisites

- JDK 25, no other installs (Maven wrapper, H2 and Playwright browsers come with the build).

## Build and test

```bash
./mvnw verify      # Flyway + jOOQ generation, Browserless and service tests, Playwright *IT tests
```

Must end with `BUILD SUCCESS` (constitution principle V).

## Run

```bash
./mvnw spring-boot:run
```

Open <http://localhost:8080>. Initial staff login: `manager` / `manager` (research R2). Reset this password
first under **Staff**.

## Manual walk-through

| # | As | Do | Expect |
|---|---|---|---|
| 1 | Manager | Create room type "Double", capacity 2, rate 120.00; create rooms 101 and 102 | both rooms listed, clean |
| 2 | Manager | Create users `anna` (Receptionist) and `ben` (Housekeeping) | both can log in, each sees only their menu |
| 3 | Guest | On `/`, search tomorrow → +2 days, 2 guests; book room 101 | reservation number shown, total 240.00 |
| 4 | Guest | Search the same dates again | room 101 not offered |
| 5 | Receptionist | Book room 102 for today → tomorrow; on Front desk check in | status checked in |
| 6 | Receptionist | Check out room 102 | room 102 dirty |
| 7 | Housekeeping | Open cleaning list (tablet width) | room 102 listed; mark cleaned → it leaves the list |
| 8 | Guest | `/my-reservation` with the number from step 3 and the email | details shown; cancel → status cancelled |
| 9 | Manager | Reports for the current month | occupancy and revenue include the night of step 5 |

Negative checks: booking room 101 for overlapping nights is rejected; check-in to a dirty room is rejected;
Housekeeping opening `manager/reports` is denied.
