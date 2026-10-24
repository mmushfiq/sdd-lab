package ch.martinelli.edu.hrs.report;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;

/**
 * One row of the revenue report: the revenue of a month per room type and its total (UC-020 step 5).
 */
public record MonthlyRevenue(YearMonth month, Map<Long, BigDecimal> revenuePerRoomType, BigDecimal total) {

    /**
     * The revenue of the room type in this month, 0.00 when it has none (UC-020 A2).
     */
    public BigDecimal revenue(Long roomTypeId) {
        return revenuePerRoomType.getOrDefault(roomTypeId, RevenueReport.ZERO);
    }
}
