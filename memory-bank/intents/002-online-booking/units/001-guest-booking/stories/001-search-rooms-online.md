---
id: 001-search-rooms-online
unit: 001-guest-booking
intent: 002-online-booking
status: complete
priority: must
created: '2026-10-29T07:58:01Z'
assigned_bolt: 004-guest-booking
implemented: true
---

# Story: 001-search-rooms-online

## User Story

**As a** guest
**I want** to see which room types are free for my dates and number of guests
**So that** I can choose a room without calling the hotel

## Acceptance Criteria

- [ ] **Given** I open `/booking` without logging in, **When** the page loads, **Then** I see a search form for arrival, departure and number of guests
- [ ] **Given** valid dates and 2 guests, **When** I search, **Then** I see each room type with capacity ≥ 2 that has at least one free room, with capacity, nightly rate, total price and number of free rooms; no room numbers
- [ ] **Given** all rooms of type "Double" are booked for my dates, **When** I search, **Then** "Double" is not listed
- [ ] **Given** an arrival in the past or departure not after arrival, **When** I search, **Then** I see a date validation error
- [ ] **Given** nothing is free, **When** I search, **Then** I see "No rooms available for these dates"
- [ ] **Given** the guest page, **When** I look for links, **Then** there is none to staff views

## Technical Notes

- Reuse the availability query of intent 001 and group by room type.
- `/booking` is the route; the page also links to "View or cancel my reservation".

## Dependencies

### Requires
- intent 001 story 002-reservation/001-search-availability

### Enables
- 002-book-room-online

## Edge Cases

| Scenario | Expected Behavior |
|----------|-------------------|
| Phone-sized screen | Form and results usable without horizontal scrolling |

## Out of Scope

- Photos and room descriptions
