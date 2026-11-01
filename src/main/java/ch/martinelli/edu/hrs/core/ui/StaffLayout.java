package ch.martinelli.edu.hrs.core.ui;

import ch.martinelli.edu.hrs.core.security.StaffUser;
import ch.martinelli.edu.hrs.room.ui.RoomTypeView;
import ch.martinelli.edu.hrs.room.ui.RoomView;
import ch.martinelli.edu.hrs.staff.ui.StaffStartView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.server.auth.AccessAnnotationChecker;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;

/**
 * Layout of all staff views; the navigation shows only the views the logged-in user may open.
 * Vaadin also checks the layout's access annotation, so it must allow every logged-in staff user.
 */
@PermitAll
public class StaffLayout extends AppLayout {

    public StaffLayout(AuthenticationContext authenticationContext, AccessAnnotationChecker accessChecker) {
        SideNav nav = new SideNav();
        nav.setId("staff-nav");
        addItem(nav, accessChecker, "Start", StaffStartView.class);
        addItem(nav, accessChecker, "Room types", RoomTypeView.class);
        addItem(nav, accessChecker, "Rooms", RoomView.class);
        addToDrawer(nav);

        Span userName = new Span(authenticationContext.getAuthenticatedUser(StaffUser.class)
                .map(StaffUser::getFullName).orElse(""));
        userName.setId("layout-user-name");
        Button logout = new Button("Log out", _ -> authenticationContext.logout());
        logout.setId("layout-logout");
        HorizontalLayout user = new HorizontalLayout(userName, logout);
        user.setAlignItems(HorizontalLayout.Alignment.CENTER);
        user.getStyle().set("margin-left", "auto").set("padding-right", "var(--lumo-space-m)");
        addToNavbar(new DrawerToggle(), user);
    }

    private static void addItem(SideNav nav, AccessAnnotationChecker accessChecker, String label,
                                Class<? extends Component> view) {
        if (accessChecker.hasAccess(view)) {
            nav.addItem(new SideNavItem(label, view));
        }
    }
}
