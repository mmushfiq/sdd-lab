package ch.martinelli.edu.hrs.reservation;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Counts failed reservation lookups per network address (UC-002 BR-003). The counts are kept in memory only.
 */
@Component
public class ReservationLookupLock {

    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final Map<String, Deque<Instant>> failedAttempts = new ConcurrentHashMap<>();
    private final Clock clock;

    public ReservationLookupLock(Clock clock) {
        this.clock = clock;
    }

    // UC-002 BR-003: Locked while 5 failed attempts from this address lie within the last 15 minutes.
    public boolean isLocked(String networkAddress) {
        Deque<Instant> attempts = failedAttempts.get(networkAddress);
        if (attempts == null) {
            return false;
        }
        synchronized (attempts) {
            removeExpired(attempts);
            return attempts.size() >= MAX_FAILED_ATTEMPTS;
        }
    }

    // UC-002 BR-003: Every failed lookup counts; a successful lookup does not reset the count.
    public void countFailedAttempt(String networkAddress) {
        Deque<Instant> attempts = failedAttempts.computeIfAbsent(networkAddress, _ -> new ArrayDeque<>());
        synchronized (attempts) {
            removeExpired(attempts);
            attempts.addLast(clock.instant());
        }
    }

    private void removeExpired(Deque<Instant> attempts) {
        Instant windowStart = clock.instant().minus(LOCK_DURATION);
        while (!attempts.isEmpty() && !attempts.peekFirst().isAfter(windowStart)) {
            attempts.removeFirst();
        }
    }
}
