package ch.martinelli.edu.hrs.report;

import java.time.LocalDate;

/**
 * The occupancy of one day of the period (UC-019 step 5).
 */
public record DailyOccupancy(LocalDate day, OccupancyFigures figures) {
}
