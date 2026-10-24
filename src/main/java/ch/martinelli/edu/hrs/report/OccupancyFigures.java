package ch.martinelli.edu.hrs.report;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Occupied and available room nights of a day, a month or the whole period, with the occupancy rate (UC-019 step 4).
 */
public record OccupancyFigures(long occupiedRoomNights, long availableRoomNights) {

    /**
     * The occupancy rate in percent, or empty when there are no available room nights (UC-019 A2).
     */
    // UC-019 BR-001: Occupied room nights divided by available room nights, in percent, rounded to one decimal place.
    public Optional<BigDecimal> occupancyRate() {
        if (availableRoomNights == 0) {
            return Optional.empty();
        }
        return Optional.of(BigDecimal.valueOf(occupiedRoomNights)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(availableRoomNights), 1, RoundingMode.HALF_UP));
    }

    // UC-019 BR-001: A month's and the period's rate come from the summed room nights, not from averaged rates.
    public OccupancyFigures plus(OccupancyFigures other) {
        return new OccupancyFigures(occupiedRoomNights + other.occupiedRoomNights,
                availableRoomNights + other.availableRoomNights);
    }
}
