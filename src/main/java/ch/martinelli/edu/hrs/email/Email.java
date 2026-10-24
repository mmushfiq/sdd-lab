package ch.martinelli.edu.hrs.email;

/**
 * A plain-text email to one recipient.
 */
public record Email(String to, String subject, String body) {
}
