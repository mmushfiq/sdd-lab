package ch.martinelli.edu.hrs.management;

import ch.martinelli.edu.hrs.email.RetryingEmailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * UC-024 Erase Guest Data: the manager erases a guest's personal data on the guest's request.
 */
@Service
public class GuestErasureService {

    private final GuestErasureRepository repository;
    private final RetryingEmailSender emailSender;
    private final TransactionTemplate transactionTemplate;

    public GuestErasureService(GuestErasureRepository repository, RetryingEmailSender emailSender,
                               TransactionTemplate transactionTemplate) {
        this.repository = repository;
        this.emailSender = emailSender;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * Step 3; empty when no guest has the email address (A1).
     */
    public Optional<GuestWithReservations> findGuest(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return repository.findByEmail(email.strip().toLowerCase(Locale.ROOT))
                .map(guest -> new GuestWithReservations(guest, repository.findReservations(guest.id())));
    }

    /**
     * Step 5.
     *
     * @throws GuestHasOpenReservationsException when the guest has Confirmed or Checked In reservations (A2)
     */
    // UC-024 BR-002: A guest with a Confirmed or Checked In reservation cannot be erased.
    public void checkErasable(Long guestId) {
        List<GuestReservation> open = openReservations(guestId);
        if (!open.isEmpty()) {
            throw new GuestHasOpenReservationsException(open);
        }
    }

    /**
     * Steps 5 and 8: checks again and erases, with the guest locked.
     *
     * @throws GuestHasOpenReservationsException when the guest has Confirmed or Checked In reservations (A2)
     */
    public void erase(Long guestId) {
        String formerEmail = transactionTemplate.execute(_ -> {
            ErasableGuest guest = repository.findForUpdate(guestId)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown guest " + guestId));
            // UC-024 BR-002: A guest with a Confirmed or Checked In reservation cannot be erased.
            checkErasable(guestId);
            // UC-024 BR-001: Only the personal data is replaced; the reservations stay for the reports.
            repository.erase(guestId);
            return guest.email();
        });
        // UC-024 step 8: Pending emails to the guest are stopped.
        emailSender.stopPendingEmailsTo(formerEmail);
    }

    private List<GuestReservation> openReservations(Long guestId) {
        return repository.findReservations(guestId).stream().filter(GuestReservation::open).toList();
    }
}
