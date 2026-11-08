package ch.martinelli.edu.hrs.reservation;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Anonymizes old guest data every night at 03:00 (FR-026, research R6).
 */
@Component
class AnonymizationJob {

    private final ReservationService reservationService;

    AnonymizationJob(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Scheduled(cron = "0 0 3 * * *")
    void anonymize() {
        reservationService.anonymizeOlderThan(ReservationService.ANONYMIZE_AFTER_MONTHS);
    }
}
