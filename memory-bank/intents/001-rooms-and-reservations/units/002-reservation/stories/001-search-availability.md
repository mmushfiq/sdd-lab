---
id: 001-search-availability
unit: 002-reservation
intent: 001-rooms-and-reservations
status: ready
priority: must
created: 2026-10-29T07:20:41Z
assigned_bolt: 002-reservation
implemented: false
---

# Story: 001-search-availability

## User Story

**As a** receptionist
**I want** to search free rooms for an arrival date, departure date and number of guests
**So that** I can offer a guest a room immediately

## Acceptance Criteria

- [ ] **Given** arrival tomorrow, departure in 3 days and 2 guests, **When** I search, **Then** I see every in-service room with capacity ≥ 2 and no overlapping confirmed reservation, with room number, type, capacity, nightly rate and total price (rate × 2 nights)
- [ ] **Given** a confirmed reservation for room 101 from the 10th to the 12th, **When** I search the 11th to the 13th, **Then** room 101 is not listed
- [ ] **Given** a confirmed reservation for room 101 departing on the 12th, **When** I search arriving on the 12th, **Then** room 101 is listed (back-to-back allowed)
- [ ] **Given** a cancelled reservation for room 101, **When** I search the same nights, **Then** room 101 is listed
- [ ] **Given** an arrival in the past, or departure not after arrival, **When** I search, **Then** I see a date validation error and no results
- [ ] **Given** no free room matches, **When** I search, **Then** I see "No rooms available for these dates"

## Technical Notes

- Model `StayPeriod(arrival, departure)` with `nights()` and `overlaps()` on the half-open interval [arrival, departure).
- One jOOQ query: bookable rooms with `NOT EXISTS` overlapping CONFIRMED reservation (`r.arrival < :departure AND r.departure > :arrival`).
- Migration creates `reservation` table in this story.

## Dependencies

### Requires
- 001-room-catalog (bookable rooms)

### Enables
- 002-create-reservation

## Edge Cases

| Scenario | Expected Behavior |
|----------|-------------------|
| 0 guests | Validation error |
| Stay of 1 night | Price = 1 × rate |
| Out-of-service room otherwise free | Not listed |

## Out of Scope

- Search by room type or price filter
