package ch.martinelli.edu.hrs.reception;

/**
 * A room of the reserved room type that is free for the stay: active, not assigned to a Checked In reservation and
 * not blocked on any night of it (UC-009 step 4 and A3). It can be assigned when it is also clean.
 */
public record RoomCandidate(Long roomId, String roomNumber, boolean clean) {
}
