package ch.martinelli.edu.hrs.core.ui;

import ch.martinelli.edu.hrs.roomcatalog.RoomTypeView;
import ch.martinelli.edu.hrs.roomcatalog.RoomView;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Layout;

/**
 * Application shell with the side navigation for all staff views.
 */
@Layout
public class MainLayout extends AppLayout {

    public MainLayout() {
        var title = new H1("HRS");
        title.getStyle().set("font-size", "var(--lumo-font-size-l)").set("margin", "0");
        addToNavbar(new DrawerToggle(), title);

        var nav = new SideNav();
        nav.addItem(new SideNavItem("Rooms", RoomView.class), new SideNavItem("Room types", RoomTypeView.class));
        addToDrawer(nav);
    }
}
