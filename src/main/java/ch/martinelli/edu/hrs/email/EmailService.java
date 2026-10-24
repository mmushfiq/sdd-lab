package ch.martinelli.edu.hrs.email;

/**
 * The external Email Service that delivers emails to guests.
 */
public interface EmailService {

    /**
     * Hands the email over to the Email Service.
     *
     * @throws EmailNotAcceptedException when the Email Service refuses the email or does not answer
     */
    void send(Email email);
}
