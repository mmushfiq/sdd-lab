package ch.martinelli.edu.hrs.reservation;

/**
 * A room type with the number of its rooms that are still available on every night of a date range.
 */
public record RoomTypeAvailability(RoomTypeOffer roomType, long availableRooms) {
}
