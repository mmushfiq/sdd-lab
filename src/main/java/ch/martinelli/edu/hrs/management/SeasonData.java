package ch.martinelli.edu.hrs.management;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * The values the manager enters for a season (UC-025 step 5): the season rate per room type id. A room type not in
 * the map has no season rate and keeps its normal rate during the season.
 */
public record SeasonData(String name, LocalDate firstNight, LocalDate lastNight, Map<Long, BigDecimal> seasonRates) {
}
