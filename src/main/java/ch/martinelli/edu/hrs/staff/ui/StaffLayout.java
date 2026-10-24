package ch.martinelli.edu.hrs.staff.ui;

import ch.martinelli.edu.hrs.reception.ui.ArrivalsAndDeparturesView;
import ch.martinelli.edu.hrs.reception.ui.CreateReservationView;
import ch.martinelli.edu.hrs.reception.ui.MissedArrivalsView;
import ch.martinelli.edu.hrs.reception.ui.RoomStatusBoardView;
import ch.martinelli.edu.hrs.reception.ui.SearchReservationsView;
import ch.martinelli.edu.hrs.security.LogoutService;
import ch.martinelli.edu.hrs.staff.StaffRole;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;

/**
 * Frame of every staff screen with the logged-in staff member and the logout (UC-023).
 */
@PermitAll
public class StaffLayout extends AppLayout {

    public StaffLayout(AuthenticationContext authenticationContext, LogoutService logoutService) {
        Span title = new Span("Hotel Reservation System");
        title.addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.FontSize.LARGE);

        Span staffMember = new Span(authenticationContext.getPrincipalName().orElse(""));
        staffMember.setId("staff-member");

        Button logOut = new Button("Log out");
        logOut.addThemeVariants(ButtonVariant.TERTIARY);
        // UC-023 step 1
        logOut.addClickListener(_ -> logoutService.logOut());

        HorizontalLayout header = new HorizontalLayout(title);
        // NFR-009: Each role sees only the screens it may use.
        if (authenticationContext.hasRole(StaffRole.RECEPTIONIST_ROLE)) {
            header.add(new RouterLink("Arrivals and Departures", ArrivalsAndDeparturesView.class),
                    // UC-007 step 1
                    new RouterLink("Reservations", SearchReservationsView.class),
                    // UC-004 step 1
                    new RouterLink("New reservation", CreateReservationView.class),
                    // UC-011 step 1
                    new RouterLink("Room status", RoomStatusBoardView.class),
                    // UC-012 step 1
                    new RouterLink("Missed arrivals", MissedArrivalsView.class));
        }
        header.add(staffMember, logOut);
        header.setWidthFull();
        // NFR-007: The header wraps instead of scrolling horizontally on a 768 px tablet.
        header.setWrap(true);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.expand(title);
        header.addClassNames(LumoUtility.Padding.Horizontal.MEDIUM);
        addToNavbar(header);
    }
}
