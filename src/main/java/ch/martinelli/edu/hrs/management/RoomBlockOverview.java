package ch.martinelli.edu.hrs.management;

import java.time.LocalDate;

/**
 * A room block as listed to the manager (UC-018 step 2).
 */
public record RoomBlockOverview(Long id, String roomNumber, LocalDate startDate, LocalDate endDate, String reason) {
}
