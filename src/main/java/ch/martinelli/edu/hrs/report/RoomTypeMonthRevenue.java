package ch.martinelli.edu.hrs.report;

import java.math.BigDecimal;

/**
 * The revenue of one room type in one month of check-out (UC-020 step 4).
 */
public record RoomTypeMonthRevenue(Long roomTypeId, String roomTypeName, Integer year, Integer month,
                                   BigDecimal revenue) {
}
