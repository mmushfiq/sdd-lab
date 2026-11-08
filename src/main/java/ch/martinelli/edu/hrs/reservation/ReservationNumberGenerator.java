package ch.martinelli.edu.hrs.reservation;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Random, not guessable reservation numbers that are easy to read out on the phone (research R4).
 */
@Component
public class ReservationNumberGenerator {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LENGTH = 8;

    private final SecureRandom random = new SecureRandom();

    public String next() {
        var number = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            number.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return number.toString();
    }
}
