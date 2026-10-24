package ch.martinelli.edu.hrs.email;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Sends an email to a guest right away and retries it every minute while the Email Service does not accept it, for
 * at most 24 hours after the event the email reports. Retries are kept in memory and are lost when the application
 * restarts. Pending emails to a recipient can be stopped (UC-024 step 8).
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
    private final Map<String, Set<PendingEmail>> pending = new ConcurrentHashMap<>();

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
        PendingEmail pendingEmail = new PendingEmail(email, recordedAt.plus(RETRY_WINDOW));
        pending.computeIfAbsent(key(email.to()), _ -> ConcurrentHashMap.newKeySet()).add(pendingEmail);
        scheduleRetry(pendingEmail);
        return false;
    }

    /**
     * Stops every email to this recipient that is waiting for a retry.
     */
    // UC-024 step 8: No pending email reaches an erased guest.
    public void stopPendingEmailsTo(String recipient) {
        Set<PendingEmail> stopped = pending.remove(key(recipient));
        if (stopped != null) {
            stopped.forEach(PendingEmail::stop);
        }
    }

    public boolean hasPendingEmailsTo(String recipient) {
        Set<PendingEmail> emails = pending.get(key(recipient));
        return emails != null && !emails.isEmpty();
    }

    private void scheduleRetry(PendingEmail pendingEmail) {
        synchronized (pendingEmail) {
            if (pendingEmail.stopped) {
                return;
            }
            pendingEmail.future = scheduler.schedule(() -> retry(pendingEmail), RETRY_INTERVAL.toMillis(),
                    TimeUnit.MILLISECONDS);
        }
    }

    private void retry(PendingEmail pendingEmail) {
        Email email = pendingEmail.email;
        if (pendingEmail.stopped) {
            return;
        }
        if (!LocalDateTime.now(clock).isBefore(pendingEmail.retryDeadline)) {
            log.warn("Gave up sending the email '{}' after {}", email.subject(), RETRY_WINDOW);
            done(pendingEmail);
            return;
        }
        if (trySend(email)) {
            done(pendingEmail);
        } else {
            scheduleRetry(pendingEmail);
        }
    }

    private void done(PendingEmail pendingEmail) {
        Set<PendingEmail> emails = pending.get(key(pendingEmail.email.to()));
        if (emails != null) {
            emails.remove(pendingEmail);
        }
    }

    private static String key(String recipient) {
        return recipient.toLowerCase(Locale.ROOT);
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

    private static final class PendingEmail {
        private final Email email;
        private final LocalDateTime retryDeadline;
        private volatile boolean stopped;
        private ScheduledFuture<?> future;

        private PendingEmail(Email email, LocalDateTime retryDeadline) {
            this.email = email;
            this.retryDeadline = retryDeadline;
        }

        private synchronized void stop() {
            stopped = true;
            if (future != null) {
                future.cancel(false);
            }
        }
    }
}
