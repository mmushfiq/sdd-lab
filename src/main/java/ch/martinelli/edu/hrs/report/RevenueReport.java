package ch.martinelli.edu.hrs.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * The revenue of a period per month and per room type, with the totals per month, per room type and for the whole
 * period in CHF (UC-020 step 5).
 */
public record RevenueReport(LocalDate firstDate, LocalDate lastDate, List<RevenueRoomType> roomTypes,
                            List<MonthlyRevenue> months, Map<Long, BigDecimal> totalPerRoomType, BigDecimal total) {

    // UC-020 BR-001: Revenue in CHF with two decimal places.
    public static final BigDecimal ZERO = new BigDecimal("0.00");

    public BigDecimal totalOf(Long roomTypeId) {
        return totalPerRoomType.getOrDefault(roomTypeId, ZERO);
    }
}
