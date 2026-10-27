package ch.martinelli.edu.hrs.reservation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * The price of the night of a reservation that begins on {@code night}, fixed when the night was recorded.
 */
public record NightPrice(LocalDate night, BigDecimal price) {

    // UC-001 BR-004: The total price is the sum of the night prices, in CHF.
    public static BigDecimal total(List<NightPrice> nightPrices) {
        return nightPrices.stream().map(NightPrice::price).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Night prices at the given nightly rates.
     */
    public static List<NightPrice> of(List<NightlyRate> rates) {
        return rates.stream().map(rate -> new NightPrice(rate.night(), rate.rate())).toList();
    }
}
