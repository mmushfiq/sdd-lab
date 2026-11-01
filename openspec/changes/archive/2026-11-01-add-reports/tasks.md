# Tasks

## 1. Report service

- [x] 1.1 Implement `ReportService.yearReport` with `MonthFigures` and `YearReport` in `ch.martinelli.edu.hrs.report`
  (design.md, decisions 1 and 2); verify with a service test: Booked and Cancelled not counted; stay from 30 March to
  2 April gives 2 nights in March and 1 in April; 60 nights in April with 10 rooms give 20.0 %; 3 × 140.00 + 2 ×
  220.00 give 860.00; a later room type rate change keeps the revenue; a year without stays gives zeros; the total
  row sums the months

## 2. Report view

- [x] 2.1 Implement `ReportView` (route `reports`, Manager only, `@AccessDeniedErrorRouter`, in `StaffLayout`) with
  year `Select`, month grid and total footer; verify `./mvnw compile` succeeds
- [x] 2.2 Write a Browserless test for `ReportView`: as `manager` the current year is preselected with twelve rows
  and a total; choosing another year reloads the figures; values are formatted as `20.0 %` and `CHF 860.00`;
  `reception` gets "Access denied"; verify `./mvnw test` passes

## 3. End-to-end

- [x] 3.1 Write `ReportIT` extending `PlaywrightIT`: insert two Checked out reservations in the current year with
  jOOQ, log in as `manager`, open "Reports" and see the expected room-nights, occupancy and revenue in their month;
  verify `./mvnw verify -Dit.test=ReportIT` passes
- [x] 3.2 Run `./mvnw verify`; verify the build ends green

## Workflow follow-up

- Run `/opsx:verify add-reports`, then `/opsx:archive add-reports`.
