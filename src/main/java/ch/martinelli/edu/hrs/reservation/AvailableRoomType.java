package ch.martinelli.edu.hrs.reservation;

import java.math.BigDecimal;
import java.util.List;

/**
 * A room type that is available for every night of a searched date range, with the lowest and the highest nightly
 * rate of the nights, the night price of every night and the total price for that range (UC-001 steps 3 and 5).
 */
public record AvailableRoomType(Long roomTypeId, String name, String description, int maxGuests,
                                BigDecimal lowestNightlyRate, BigDecimal highestNightlyRate, long nights,
                                List<NightPrice> nightPrices, BigDecimal totalPrice) {
}
