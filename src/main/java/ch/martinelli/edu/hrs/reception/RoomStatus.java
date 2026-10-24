package ch.martinelli.edu.hrs.reception;

import java.time.LocalDate;

/**
 * An active room on the room status board (UC-011 steps 2 and 3).
 *
 * @param guestName     the guest of the Checked In reservation the room is assigned to, or null
 * @param departureDate the departure date of that reservation, or null
 * @param blockedUntil  the last blocked date of the room block covering today, or null
 * @param blockReason   the reason of that room block, or null
 */
public record RoomStatus(Long roomId, String roomNumber, Long roomTypeId, String roomTypeName, boolean clean,
                         String guestName, LocalDate departureDate, LocalDate blockedUntil, String blockReason) {

    // UC-011 BR-001: Occupied when assigned to a Checked In reservation, otherwise Blocked when a block covers today,
    // otherwise Free.
    public Occupancy occupancy() {
        if (guestName != null) {
            return Occupancy.OCCUPIED;
        }
        return blockedUntil != null ? Occupancy.BLOCKED : Occupancy.FREE;
    }

    // UC-011 BR-001: An occupied room that is also blocked today is marked.
    public boolean occupiedAndBlocked() {
        return occupancy() == Occupancy.OCCUPIED && blockedUntil != null;
    }

    // UC-011 BR-002: Free and Clean rooms are assignable.
    public boolean assignable() {
        return occupancy() == Occupancy.FREE && clean;
    }
}
