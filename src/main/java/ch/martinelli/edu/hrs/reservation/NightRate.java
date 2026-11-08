package ch.martinelli.edu.hrs.reservation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * The rate of one night of a stay; the night belongs to the date it starts on (003 FR-004, research R3).
 */
public record NightRate(LocalDate night, BigDecimal rate) {

    public static BigDecimal total(List<NightRate> nights) {
        return nights.stream().map(NightRate::rate).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Consecutive nights with the same rate as one group, e.g. "1 night × 100.00, 2 nights × 150.00"
     * (contracts/ui-changes.md).
     */
    public static String pricePerNight(List<NightRate> nights) {
        var groups = new ArrayList<String>();
        for (int i = 0; i < nights.size(); ) {
            var rate = nights.get(i).rate();
            var count = 0;
            while (i < nights.size() && nights.get(i).rate().compareTo(rate) == 0) {
                count++;
                i++;
            }
            groups.add(count + (count == 1 ? " night × " : " nights × ") + rate.toPlainString());
        }
        return String.join(", ", groups);
    }
}
