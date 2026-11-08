package ch.martinelli.edu.hrs.core;

/**
 * A business rule was violated; the message is shown to the user as is (research R11).
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
