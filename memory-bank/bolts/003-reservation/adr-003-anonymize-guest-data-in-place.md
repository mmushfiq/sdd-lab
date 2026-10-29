---
bolt: 003-reservation
created: 2026-10-29T11:48:01Z
status: accepted
superseded_by: null
---

# ADR-003: Anonymize guest data in place

## Context

Guests may ask the hotel to erase their personal data (GDPR right to erasure, FR-8). Reports (intent 005) still need occupancy and revenue per reservation, and the booking number must stay unique and traceable for accounting.

## Decision

Anonymization overwrites `guest_name`, `guest_email` and `guest_phone` of one reservation with the placeholder "Anonymized" and sets `anonymized = true`. The row, booking number, room, dates, number of guests, price and status stay. It is allowed only for CANCELLED reservations or reservations whose departure is in the past, is confirmed by the user, cannot be undone, and the removed values are never logged.

## Rationale

Overwriting in place removes the personal data while keeping every fact reports need, with no schema change and no extra table. The `anonymized` flag lets the UI and later rules tell an anonymized reservation from a guest who happens to be called "Anonymized".

### Alternatives Considered

| Alternative | Pros | Cons | Why Rejected |
|-------------|------|------|--------------|
| Delete the reservation | Simplest erasure | Loses occupancy and revenue history; gaps in booking numbers for accounting | Reports and accounting need the stay |
| Separate guest table, delete the guest row | Clean separation of personal data | Extra table and joins everywhere for one rule; guest data is per reservation anyway | More schema and code than the requirement needs |
| Set columns to NULL | No placeholder text | NOT NULL constraints and every view must handle null names | Placeholder is simpler and readable in lists |

## Consequences

### Positive

- Personal data gone, reporting data intact
- No migration; the `anonymized` column exists since V2

### Negative

- Anonymization is per reservation; a guest with several reservations needs several actions (bulk by email is out of scope)
- Backups still contain the old data until they expire (outside the app)

### Risks

- A future feature copies guest data elsewhere (emails, exports) and forgets to anonymize it. Mitigation: new copies of guest data must reference this ADR and be cleared by `anonymizeGuest`.

## Related

- **Stories**: 002-reservation/007-anonymize-guest-data
- **Standards**: logging rule "never log personal guest data" in `standards/coding-standards.md`
- **Previous ADRs**: ADR-001 (anonymize locks the reservation row like other lifecycle changes)
