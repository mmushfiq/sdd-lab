package ch.martinelli.edu.hrs.core.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;

/**
 * Staff layout: side menu with the views the logged-in user may open (research R9) and a logout button.
 */
@PermitAll
public class MainLayout extends AppLayout {

    public MainLayout(AuthenticationContext authenticationContext) {
        var title = new H1("Hotel Reservation System");
        title.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        var logout = new Button("Log out", _ -> authenticationContext.logout());
        var header = new HorizontalLayout(new DrawerToggle(), title, logout);
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.expand(title);
        header.addClassNames(LumoUtility.Padding.End.MEDIUM);
        addToNavbar(header);

        var nav = new SideNav();
        MenuConfiguration.getMenuEntries()
                .forEach(entry -> nav.addItem(new SideNavItem(entry.title(), entry.path())));
        addToDrawer(nav);
    }
}
