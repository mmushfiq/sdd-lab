package ch.martinelli.edu.hrs.email;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Sends an email to a guest right away and retries it every minute while the Email Service does not accept it, for
 * at most 24 hours after the event the email reports. Retries are kept in memory and are lost when the application
 * restarts.
 */
@Component
public class RetryingEmailSender {

    private static final Logger log = LoggerFactory.getLogger(RetryingEmailSender.class);

    public static final Duration RETRY_WINDOW = Duration.ofHours(24);
    public static final Duration RETRY_INTERVAL = Duration.ofMinutes(1);

    private final EmailService emailService;
    private final Clock clock;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().name("email-retry").daemon().factory());

    public RetryingEmailSender(EmailService emailService, Clock clock) {
        this.emailService = emailService;
        this.clock = clock;
    }

    /**
     * @param recordedAt when the reported event was recorded; retries stop 24 hours later
     * @return true when the Email Service accepted the email; false when it did not and the email will be retried
     */
    public boolean send(Email email, LocalDateTime recordedAt) {
        if (trySend(email)) {
            return true;
        }
        scheduleRetry(email, recordedAt.plus(RETRY_WINDOW));
        return false;
    }

    private void scheduleRetry(Email email, LocalDateTime retryDeadline) {
        scheduler.schedule(() -> {
            if (!LocalDateTime.now(clock).isBefore(retryDeadline)) {
                log.warn("Gave up sending the email '{}' after {}", email.subject(), RETRY_WINDOW);
                return;
            }
            if (!trySend(email)) {
                scheduleRetry(email, retryDeadline);
            }
        }, RETRY_INTERVAL.toMillis(), TimeUnit.MILLISECONDS);
    }

    private boolean trySend(Email email) {
        try {
            emailService.send(email);
            return true;
        } catch (RuntimeException e) {
            log.warn("The Email Service did not accept the email '{}': {}", email.subject(), e.getMessage());
            return false;
        }
    }

    @PreDestroy
    void shutdown() {
        scheduler.shutdownNow();
    }
}
