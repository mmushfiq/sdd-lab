package ch.martinelli.edu.hrs.reservation;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The nightly rate of a room type for the night that begins on {@code night} (UC-025 BR-003).
 */
public record NightlyRate(LocalDate night, BigDecimal rate) {
}
