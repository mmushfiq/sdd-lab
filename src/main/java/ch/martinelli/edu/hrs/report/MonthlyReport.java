package ch.martinelli.edu.hrs.report;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Occupancy, revenue and online share of one month (FR-021, data-model.md "Derived values").
 */
public record MonthlyReport(int occupiedNights, int availableNights, BigDecimal revenue,
                            int createdReservations, int onlineReservations) {

    public BigDecimal occupancyPercent() {
        return percent(occupiedNights, availableNights);
    }

    /**
     * Null when no reservation was created in the month.
     */
    public BigDecimal onlineSharePercent() {
        return percent(onlineReservations, createdReservations);
    }

    private static BigDecimal percent(int part, int whole) {
        if (whole == 0) {
            return null;
        }
        return BigDecimal.valueOf(part * 100L).divide(BigDecimal.valueOf(whole), 1, RoundingMode.HALF_UP);
    }
}
