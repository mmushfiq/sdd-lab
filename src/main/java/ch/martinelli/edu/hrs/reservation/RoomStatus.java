package ch.martinelli.edu.hrs.reservation;

/**
 * One row of the room overview for today (FR-019).
 */
public record RoomStatus(String roomNumber, String roomTypeName, Occupancy occupancy, String cleaningStatus) {
}
