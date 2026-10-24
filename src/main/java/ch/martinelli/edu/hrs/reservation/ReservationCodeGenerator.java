package ch.martinelli.edu.hrs.reservation;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Creates reservation codes.
 */
@Component
public class ReservationCodeGenerator {

    // Letters and digits without the easily confused 0, O, 1 and I
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    static final int LENGTH = 10;

    private final SecureRandom random = new SecureRandom();

    // UC-001 BR-007: At least 8 random characters that cannot be derived from other reservation data.
    public String newCode() {
        StringBuilder code = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
