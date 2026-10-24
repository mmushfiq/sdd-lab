package ch.martinelli.edu.hrs.reservation;

import ch.martinelli.edu.hrs.core.MutableClock;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Test stand-in for the lookup lock: forgets every failed attempt on {@link #reset()} and has its own clock, so a
 * test can let the 15 minutes pass without changing "today" for the rest of the application.
 */
public class ResettableReservationLookupLock extends ReservationLookupLock {

    private final Instant start;
    private final MutableClock lockClock;
    private volatile ReservationLookupLock delegate;

    public ResettableReservationLookupLock(Clock clock) {
        super(clock);
        this.start = clock.instant();
        this.lockClock = new MutableClock(start, clock.getZone());
        this.delegate = new ReservationLookupLock(lockClock);
    }

    @Override
    public boolean isLocked(String networkAddress) {
        return delegate.isLocked(networkAddress);
    }

    @Override
    public void countFailedAttempt(String networkAddress) {
        delegate.countFailedAttempt(networkAddress);
    }

    public void advance(Duration duration) {
        lockClock.advance(duration);
    }

    public void reset() {
        lockClock.setInstant(start);
        delegate = new ReservationLookupLock(lockClock);
    }
}
