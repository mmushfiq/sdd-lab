package ch.martinelli.edu.hrs.management;

/**
 * A room in the manager's list (UC-017 step 2).
 */
public record RoomOverview(Long id, String roomNumber, Long roomTypeId, String roomTypeName, boolean clean,
                           boolean active) {
}
