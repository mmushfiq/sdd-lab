package ch.martinelli.edu.hrs.staff.ui;

import ch.martinelli.edu.hrs.core.security.StaffUser;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;

@Route(StaffStartView.ROUTE)
@PageTitle("Staff")
@PermitAll
public class StaffStartView extends VerticalLayout {

    public static final String ROUTE = "staff";

    public StaffStartView(AuthenticationContext authenticationContext) {
        StaffUser user = authenticationContext.getAuthenticatedUser(StaffUser.class).orElseThrow();

        Paragraph fullName = new Paragraph(user.getFullName());
        fullName.setId("staff-full-name");
        Paragraph role = new Paragraph(user.getRoleName());
        role.setId("staff-role");
        Button logout = new Button("Log out", _ -> authenticationContext.logout());
        logout.setId("logout");

        add(new H1("Hotel Reservation System"), fullName, role, logout);
    }
}
