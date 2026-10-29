---
stage: test
bolt: 001-room-catalog
created: 2026-10-29T10:35:36Z
---

## Test Report: room-catalog

### Summary

- **Tests**: 29/29 passed (`./mvnw verify`, BUILD SUCCESS)
- **Coverage**: not measured (no coverage tool in the build); every acceptance criterion has at least one test

### Test Files

- [x] `src/test/java/ch/martinelli/edu/hrs/roomcatalog/RoomTypeViewTest.java` - 16 Browserless tests: list, create, trim, duplicate name, required name, capacity and rate validation, edit rate, delete / cancel / in use / deleted meanwhile
- [x] `src/test/java/ch/martinelli/edu/hrs/roomcatalog/RoomViewTest.java` - 9 Browserless tests: list with type details, create (trimmed), duplicate number, number length, required type, change type, delete / cancel, out of service and back
- [x] `src/test/java/ch/martinelli/edu/hrs/roomcatalog/RoomCatalogServiceTest.java` - 3 service tests for `findBookableRooms`: capacity filter, out-of-service excluded, empty when all out of service

All tests run in a rolled-back transaction against the real Spring context and H2 with the seeded data.

### Acceptance Criteria Validation

Story 001 - manage room types
- ✅ **List sorted by name with capacity and rate**: `listsRoomTypesSortedByName`
- ✅ **Valid room type appears in list**: `createsRoomType` (uses "Suite"; "Double" is already seeded)
- ✅ **"double" rejected with "Name already exists"**: `rejectsDuplicateNameIgnoringCase`
- ✅ **Capacity 0 or 11 rejected**: `rejectsCapacityOutOfRange`
- ✅ **Rate 0, negative, more than two decimals rejected**: `rejectsInvalidRate`
- ✅ **Changed rate shown in list**: `updatesRate`
- ✅ **Name trimmed; empty name required**: `trimsName`, `requiresName`

Story 002 - delete room type
- ✅ **Unused type deleted after confirmation**: `deletesUnusedRoomTypeAfterConfirmation`
- ✅ **Type used by rooms cannot be deleted**: `refusesToDeleteRoomTypeUsedByRooms`
- ✅ **Cancel deletes nothing**: `keepsRoomTypeWhenDeleteIsCancelled`
- ✅ **Type deleted meanwhile reported, list refreshed**: `reportsRoomTypeDeletedMeanwhile`

Story 003 - manage rooms
- ✅ **List sorted by number with type, capacity, rate, state**: `listsRoomsSortedByNumberWithTypeDetails`
- ✅ **Room "111" / "Double" appears in list**: `createsRoom` (also checks trimming)
- ✅ **Duplicate "101" rejected**: `rejectsDuplicateRoomNumber`
- ✅ **Room type required**: `requiresRoomType`
- ✅ **Changed type shown in list**: `changesRoomType`
- ✅ **Room deleted after confirmation**: `deletesRoomAfterConfirmation`, cancel in `keepsRoomWhenDeleteIsCancelled`
- ✅ **Number longer than 10 characters rejected**: `rejectsRoomNumberLongerThanTenCharacters`

Story 004 - out of service
- ✅ **Out of service and back in service shown in list**: `setsRoomOutOfServiceAndBackInService`
- ✅ **Out-of-service rooms not bookable; empty when all out of service**: `bookableRoomsExcludeOutOfServiceRooms`, `noBookableRoomsWhenAllAreOutOfService`

### Issues Found

- None in production code. One test helper was changed: navigating to the view already shown reuses the same view instance, so test data is now created through the form instead of the service.

### Notes

- No browser (Playwright) test in this bolt; the views were not checked manually in a browser.
