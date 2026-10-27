package ch.martinelli.edu.hrs.management;

import java.time.LocalDate;

/**
 * One season in the list of seasons (UC-025 step 2).
 */
public record SeasonOverview(Long id, String name, LocalDate firstNight, LocalDate lastNight, int roomTypesWithRate) {
}
