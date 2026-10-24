package ch.martinelli.edu.hrs.email;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Test stand-in for the Email Service: records the accepted emails and can be told to refuse them.
 */
public class RecordingEmailService implements EmailService {

    private final List<Email> sentEmails = new CopyOnWriteArrayList<>();
    private volatile boolean refusing;

    @Override
    public void send(Email email) {
        if (refusing) {
            throw new EmailNotAcceptedException("The Email Service refuses emails", null);
        }
        sentEmails.add(email);
    }

    public List<Email> sentEmails() {
        return List.copyOf(sentEmails);
    }

    public void setRefusing(boolean refusing) {
        this.refusing = refusing;
    }

    public void reset() {
        sentEmails.clear();
        refusing = false;
    }
}
