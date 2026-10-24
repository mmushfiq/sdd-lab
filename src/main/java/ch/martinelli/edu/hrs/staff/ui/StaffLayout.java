package ch.martinelli.edu.hrs.staff.ui;

import ch.martinelli.edu.hrs.security.LogoutService;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
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

        HorizontalLayout header = new HorizontalLayout(title, staffMember, logOut);
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.expand(title);
        header.addClassNames(LumoUtility.Padding.Horizontal.MEDIUM);
        addToNavbar(header);
    }
}
