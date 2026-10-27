package ch.martinelli.edu.hrs.management;

import java.math.BigDecimal;

/**
 * A room type shown in a season with its normal rate and its season rate, if one is set (UC-025 step 4, BR-006).
 */
public record SeasonRoomType(Long roomTypeId, String name, boolean active, BigDecimal normalRate,
                             BigDecimal seasonRate) {
}
