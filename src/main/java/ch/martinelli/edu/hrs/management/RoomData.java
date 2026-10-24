package ch.martinelli.edu.hrs.management;

/**
 * The values a manager enters for a room (UC-017 step 4).
 */
public record RoomData(String roomNumber, Long roomTypeId, boolean active) {
}
