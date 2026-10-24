package ch.martinelli.edu.hrs.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Stand-in for the Email Service: writes every email to the log instead of delivering it.
 * Replace it with an SMTP-backed implementation before going live.
 */
@Service
public class LoggingEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailService.class);

    @Override
    public void send(Email email) {
        log.info("Email to {} with subject '{}':\n{}", email.to(), email.subject(), email.body());
    }
}
