# Quickstart: Validate Seasonal Room Rates

## Prerequisites

- Java 25; run from the repository root.

## Automated

```bash
./mvnw compile   # runs V5 (and V6 once added), regenerates jOOQ (seasonal_rate, reservation_night)
./mvnw test      # NightRate unit tests, service tests, migration test, Browserless view tests
./mvnw verify    # plus Playwright ITs; the feature is done only when this is green
```

## By hand

1. Start the app (`./mvnw spring-boot:run`). Existing reservations in `./data/hrs` keep their prices: each of
   their nights gets their old nightly rate (FR-008).
2. **Seasonal rate (US1)**: log in as `manager`, open "Seasonal rates", add "Summer" for a room type with
   standard rate 100: from 1 July to 31 August next year, rate 150. It appears in the grid.
3. Add "Overlap" for the same room type from 30 August to 5 September: rejected with
   `Overlaps the seasonal rate "Summer" (…)` (US1/AC3, AC5). Save without a name: rejected (US1/AC4).
4. **Prices (US2)**: on `/` search 10–13 July: total 450; search 30 June – 2 July: total 250. The search grid
   has no "Nightly rate" column.
5. Book 30 June – 3 July and look it up on `/my-reservation`: "Price per night: 1 night × 100.00, 2 nights ×
   150.00", "Total price: 400.00" (US2/AC5). The confirmation email shows 400.00.
6. **Keep prices (US3)**: change "Summer" to 200. Look the reservation up again: still 400.00 (US3/AC1).
7. As Receptionist on `reception/reservations`, find it: the "Price per night" column shows the same text.
   Extend the departure to 4 July: the new night gets 200, the others keep their rates, total 600 (US3/AC3).
   Move it to a room of another room type: every night is priced at that type's current rates (US3/AC4).
8. **Revenue (US4)**: check in and check out a stay across 30 June / 1 July (test data, `ReportServiceTest`);
   the June and July reports each count only their own nights at their saved rates.
9. As Receptionist or Housekeeping, open `manager/seasonal-rates`: access denied (US1/AC6).

Data model and examples: `data-model.md`. Screen changes: `contracts/ui-changes.md`.
