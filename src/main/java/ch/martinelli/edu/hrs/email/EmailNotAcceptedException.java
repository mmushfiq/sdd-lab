package ch.martinelli.edu.hrs.email;

/**
 * Thrown when the Email Service refuses an email or does not answer.
 */
public class EmailNotAcceptedException extends RuntimeException {

    public EmailNotAcceptedException(String message, Throwable cause) {
        super(message, cause);
    }
}
