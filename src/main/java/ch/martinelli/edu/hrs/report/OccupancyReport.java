package ch.martinelli.edu.hrs.report;

import java.time.LocalDate;
import java.util.List;

/**
 * The occupancy per day, per month and for the whole period (UC-019 step 5).
 */
public record OccupancyReport(LocalDate firstDate, LocalDate lastDate, List<DailyOccupancy> days,
                              List<MonthlyOccupancy> months, OccupancyFigures period) {
}
