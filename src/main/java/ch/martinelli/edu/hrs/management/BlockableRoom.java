package ch.martinelli.edu.hrs.management;

/**
 * An active room the manager can block (UC-018 step 3).
 */
public record BlockableRoom(Long id, String roomNumber, Long roomTypeId, String roomTypeName) {
}
