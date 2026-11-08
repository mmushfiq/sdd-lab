package ch.martinelli.edu.hrs.guest;

import ch.martinelli.edu.hrs.core.BusinessRuleException;
import ch.martinelli.edu.hrs.reservation.GuestReservation;
import ch.martinelli.edu.hrs.reservation.NightRate;
import ch.martinelli.edu.hrs.reservation.ReservationService;
import ch.martinelli.edu.hrs.reservation.ReservationStatus;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import static ch.martinelli.edu.hrs.core.ui.Notifications.showError;

/**
 * A guest looks up their reservation with its number and the booking email and may cancel it (User Story 7).
 */
@Route("my-reservation")
@PageTitle("My reservation | Hotel Reservation System")
@AnonymousAllowed
public class MyReservationView extends VerticalLayout {

    private final transient ReservationService reservationService;
    private final TextField number = new TextField("Reservation number");
    private final EmailField email = new EmailField("Email");
    private final VerticalLayout details = new VerticalLayout();

    public MyReservationView(ReservationService reservationService) {
        this.reservationService = reservationService;
        number.setMaxLength(8);
        email.setMaxLength(254);
        var show = new Button("Show reservation", _ -> lookUp());
        show.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        var form = new HorizontalLayout(number, email, show);
        form.setAlignItems(FlexComponent.Alignment.BASELINE);
        form.setWrap(true);
        details.setPadding(false);

        add(new H1("My reservation"), form, details);
    }

    private void lookUp() {
        details.removeAll();
        reservationService.findForGuest(number.getValue(), email.getValue()).ifPresentOrElse(this::show,
                // Same message whether or not the number exists (US7/AC2)
                () -> details.add(new Paragraph(ReservationService.NOT_FOUND)));
    }

    private void show(GuestReservation reservation) {
        var info = new FormLayout();
        info.addFormItem(new Paragraph(reservation.reservationNumber()), "Reservation number");
        info.addFormItem(new Paragraph(reservation.arrival().toString()), "Arrival");
        info.addFormItem(new Paragraph(reservation.departure().toString()), "Departure");
        info.addFormItem(new Paragraph(reservation.roomTypeName()), "Room type");
        info.addFormItem(new Paragraph(NightRate.pricePerNight(reservation.nights())), "Price per night");
        info.addFormItem(new Paragraph(reservation.totalPrice().toPlainString()), "Total price");
        info.addFormItem(new Paragraph(label(reservation.status().name())), "Status");
        details.add(info);
        if (reservation.cancellable()) {
            var deadline = new Paragraph(reservation.cancellationDeadline().text());
            deadline.setId("cancellation-deadline");
            details.add(deadline);
            var cancel = new Button("Cancel reservation", _ -> confirmCancel());
            cancel.addThemeVariants(ButtonVariant.LUMO_ERROR);
            details.add(cancel);
        } else if (reservation.status() == ReservationStatus.BOOKED) {
            var passed = new Paragraph(reservation.cancellationDeadline().passedText());
            passed.setId("cancellation-deadline");
            details.add(passed);
        }
    }

    private void confirmCancel() {
        var confirm = new ConfirmDialog("Cancel your reservation?", "This cannot be undone.",
                "Cancel reservation", _ -> {
            try {
                reservationService.cancelByGuest(number.getValue(), email.getValue());
                Notification.show("Your reservation is cancelled.", 5000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (BusinessRuleException e) {
                showError(e.getMessage());
            }
            lookUp();
        });
        confirm.setConfirmButtonTheme("error primary");
        confirm.setCancelable(true);
        confirm.setCancelText("Keep reservation");
        confirm.open();
    }

    /**
     * CHECKED_IN → Checked in.
     */
    private static String label(String constant) {
        return constant.charAt(0) + constant.substring(1).toLowerCase().replace('_', ' ');
    }
}
