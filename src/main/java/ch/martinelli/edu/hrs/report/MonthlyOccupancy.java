package ch.martinelli.edu.hrs.report;

import java.time.YearMonth;

/**
 * The occupancy of one month, counting only the days of the month that are in the period (UC-019 step 5, BR-004).
 */
public record MonthlyOccupancy(YearMonth month, OccupancyFigures figures) {
}
