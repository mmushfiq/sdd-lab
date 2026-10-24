package ch.martinelli.edu.hrs.management;

import java.time.LocalDate;

/**
 * The values the manager enters for a new room block (UC-018 step 3).
 */
public record RoomBlockData(Long roomId, LocalDate startDate, LocalDate endDate, String reason) {
}
